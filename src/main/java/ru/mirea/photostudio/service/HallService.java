package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.repository.HallRepository;

import java.util.List;

/** Справочник залов. */
public class HallService {

    private final HallRepository hallRepository;

    public HallService(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    public List<Hall> getAll() {
        return hallRepository.findAll();
    }

    public Hall getById(int id) {
        return hallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Зал", id));
    }
}
