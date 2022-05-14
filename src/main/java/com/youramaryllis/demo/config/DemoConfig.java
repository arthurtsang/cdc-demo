package com.youramaryllis.demo.config;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import java.net.UnknownHostException;

@Slf4j
@Configuration
public class DemoConfig {
    @Bean
    public io.debezium.config.Configuration demoConnector(DebeziumConfig config) {
        return io.debezium.config.Configuration.create()
                .with("name", config.name)
                .with("connector.class", config.connectorClass )
                .with("tasks.max", config.tasksMax)
                .with("plugin.name", config.pluginName)
                .with("offset.storage", config.offset.storage)
                .with("offset.storage.file.filename", config.offset.storageFilePath.toString())
                .with("offset.flush.interval.ms", config.offset.flushIntervalMS)
                .with("database.hostname", config.database.hostname)
                .with("database.port", config.database.port)
                .with("database.user", config.database.user)
                .with("database.password", config.database.password)
                .with("database.dbname", config.database.dbname)
                .with("database.server.name", config.database.hostname+"-"+config.database.dbname)
                .with("database.history", config.database.historyClass)
                .with("database.history.file.filename", config.database.historyFile.toString())
                .with("table.blacklist", config.tableBlacklist)
                .build();
    }

    @Bean
    public RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {

        return new RegistryEventConsumer<Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher()
                        .onEvent(event -> log.info(event.toString()));
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
            }
        };
    }

    @Bean
    public RestHighLevelClient elasticsearchClient(ElasticsearchConfig config) throws UnknownHostException {
        HttpHost[] httpHosts = config.getPorts().stream().map(p -> new HttpHost(config.getHost(), p)).toArray(HttpHost[]::new);
        return new RestHighLevelClient(RestClient.builder( httpHosts ));
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }
}
