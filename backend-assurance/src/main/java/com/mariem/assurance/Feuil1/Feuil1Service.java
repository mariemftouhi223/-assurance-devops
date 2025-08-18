package com.mariem.assurance.Feuil1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Feuil1Service {

    @Autowired
    private Feuil1Repository feuil1Repository;

    public List<Feuil1> getAllData() {
        return feuil1Repository.findAll();
    }
}
