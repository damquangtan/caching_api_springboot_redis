package com.example.cached.controller;

import com.example.cached.entities.Tutorial;
import com.example.cached.services.TutorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class TutorialController {

    @Autowired
    private TutorialService tutorialService;

    @GetMapping(value = "/tutorials")
    public ResponseEntity<List<Tutorial>> getTutorials() {
        return new ResponseEntity<>(tutorialService.getTutorials(), HttpStatus.OK);
    }

    @PostMapping(value = "/tutorials")
    public ResponseEntity<Tutorial> createTutorial(@RequestBody Tutorial tutorial) {
        return new ResponseEntity<>(tutorialService.createTutorial(tutorial), HttpStatus.CREATED);
    }
}
