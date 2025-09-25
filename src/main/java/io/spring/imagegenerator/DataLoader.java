package io.spring.imagegenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.spring.imagegenerator.service.CsvDataService;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private CsvDataService csvDataService;

    @Override
    public void run(String... args) throws Exception {
        csvDataService.loadAllDataFromCsv();
    }
}
