package com.sythel.censor.logging;

import java.util.List;

public interface LogStorage {

    void save(LogEntry entry);

    List<LogEntry> findAll();

    void close();
}