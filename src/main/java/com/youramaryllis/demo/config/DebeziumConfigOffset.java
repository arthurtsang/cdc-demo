package com.youramaryllis.demo.config;

import lombok.Data;

import java.nio.file.Path;

@Data
public class DebeziumConfigOffset {
    String storage;
    Path storageFilePath;
    int flushIntervalMS;
}
