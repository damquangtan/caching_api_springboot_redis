package com.example.cached.services;

import com.example.cached.entities.Tutorial;
import com.example.cached.repo.TutorialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TutorialServiceImpl implements TutorialService {

    @Autowired
    private TutorialRepository repository;

    @Override
    public List<Tutorial> getTutorials() {
        return repository.findAll();
    }

    @Override
    public Tutorial createTutorial(Tutorial tutorial) {
        return repository.save(tutorial);
    }
}
