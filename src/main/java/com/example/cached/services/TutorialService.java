package com.example.cached.services;

import com.example.cached.entities.Tutorial;
import java.util.List;

public interface TutorialService {
    List<Tutorial> getTutorials();
    Tutorial createTutorial(Tutorial tutorial);
}
