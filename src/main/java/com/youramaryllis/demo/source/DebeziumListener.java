package com.youramaryllis.demo.source;

import com.youramaryllis.demo.destination.DemoService;
import io.debezium.DebeziumException;
import io.debezium.config.Configuration;
import io.debezium.data.Envelope.Operation;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static io.debezium.data.Envelope.FieldName.AFTER;
import static io.debezium.data.Envelope.FieldName.BEFORE;
import static io.debezium.data.Envelope.FieldName.OPERATION;

@Component
@Slf4j
public class DebeziumListener {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;
    private final DemoService demoService;

    private DebeziumListener(Configuration demoConnector, DemoService demoService ) {
        engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(demoConnector.asProperties())
                .notifying(this::handleBatch).build();
        this.demoService = demoService;
    }

    @RequiredArgsConstructor
    static
    class RetryDebeziumRunnable implements Runnable {
        private final DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

        @Retry(name = "debezium")
        @Override
        public void run() {
            try {
                engine.run();
                log.info("******** debezium exiting!!");
            }catch (Exception e) {
                log.error( "debezium threw exception: ", e );
                DebeziumException de = new DebeziumException();
                de.initCause(e);
                throw de;
            }
            throw new DebeziumException();
        }
    }

    @PostConstruct
    private void start() {
        this.executor.execute(new RetryDebeziumRunnable(engine));
    }

    @SneakyThrows
    @PreDestroy
    private void stop() {
        if( engine != null ) engine.close();
        executor.shutdownNow();
    }

    /**
     * get the change event in Kafka's SourceRecord for parsing
     * READ operation when kafka's offset is behind the current data
     * Debezium will simply close itself when this handler threw exception, catching all exception, just make sure it's not mark processed so it will be process again.
     * @param records
     * @param committer
     */
    private void handleBatch( List<RecordChangeEvent<SourceRecord>> records, DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> committer ) {
        records.forEach( r -> {
            try {
                Struct sourceRecordValue = (Struct) r.record().value();
                if (sourceRecordValue != null) {
                    Operation operation = Operation.forCode((String) sourceRecordValue.get(OPERATION));
                    if (operation != Operation.READ) {
                        Map<String, Object> message;
                        String record = AFTER;
                        if (operation == Operation.DELETE) record = BEFORE;
                        Struct struct = (Struct) sourceRecordValue.get(record);
                        String table = ((Struct) sourceRecordValue.get("source")).get("table").toString();
                        message = struct.schema().fields().stream()
                                .map(Field::name)
                                .filter(fieldName -> struct.get(fieldName) != null)
                                .map(fieldName -> {
                                    String key = (fieldName.equals(table + "_id")) ? "id" : fieldName;
                                    return new AbstractMap.SimpleEntry<>(key, struct.get(fieldName));
                                })
                                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
                        log.info("Data Changed: {} with Operation: {}", message, operation);
                        demoService.updateIndex(table, message, operation);
                        log.info("Index Updated: {} with Operation: {}", message, operation);
                    }
                }
                // probably should stop the loop if there's exception
                committer.markProcessed(r);
            } catch (Throwable t) {
                log.error("fail handling debezium change event", t);
            }
        });
    }
}
