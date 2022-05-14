package com.youramaryllis.demo.destination;

import io.debezium.data.Envelope;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class DemoService {
    @Autowired
    RestHighLevelClient client;

    @SneakyThrows
    public void updateIndex(String index, Map<String, Object> message, Envelope.Operation operation) {
        String id = (String) message.get("id");
        message.remove("id");
        switch ( operation ) {
            case CREATE:
            case UPDATE:
                IndexRequest request = new IndexRequest(index).id(id);
                request.source(message);
                IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                log.info( "insert response: " + response.status() );
                break;
            case DELETE:
                DeleteRequest drequest = new DeleteRequest(index).id(id);
                DeleteResponse dresponse = client.delete(drequest, RequestOptions.DEFAULT);
                log.info( "delete response: " + dresponse.status() );
                break;
            default:
                log.error("invalid operation: " + operation);
        }
    }
}
