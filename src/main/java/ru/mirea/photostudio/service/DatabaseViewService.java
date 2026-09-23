package ru.mirea.photostudio.service;

import ru.mirea.photostudio.model.TableData;
import ru.mirea.photostudio.repository.DatabaseViewRepository;

import java.util.List;

/** Прослойка между консольным меню и репозиторием: меню не обращается к репозиториям напрямую. */
public class DatabaseViewService {

    private final DatabaseViewRepository repository;

    public DatabaseViewService(DatabaseViewRepository repository) {
        this.repository = repository;
    }

    public List<TableData> readAllTables() {
        return repository.getTableNames().stream()
                .map(repository::readTable)
                .toList();
    }
}
