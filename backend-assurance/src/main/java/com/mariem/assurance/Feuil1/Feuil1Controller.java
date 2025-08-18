package com.mariem.assurance.Feuil1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/feuil1")

public class Feuil1Controller {

    @Autowired
    private Feuil1Service feuil1Service;

    @GetMapping("/all")
    public List<Feuil1> getAllFeuil1() {
        return feuil1Service.getAllData();
    }
}
