package com.youramaryllis.demo.config;

import lombok.Data;

import java.nio.file.Path;

@Data
public class DebeziumConfigDatabase {
    String hostname;
    int port;
    String user;
    String password;
    String historyClass;
    Path historyFile;
    String dbname;
}
