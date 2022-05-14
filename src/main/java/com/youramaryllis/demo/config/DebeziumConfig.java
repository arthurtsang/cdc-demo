package com.youramaryllis.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("debezium")
@Data
public class DebeziumConfig {
    String name;
    String connectorClass;
    int tasksMax;
    String pluginName;
    DebeziumConfigOffset offset;
    DebeziumConfigDatabase database;
    String tableBlacklist;
}
