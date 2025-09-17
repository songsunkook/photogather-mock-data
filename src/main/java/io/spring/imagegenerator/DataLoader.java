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
    
    @Value("${csv.data.path}")
    private String csvDataPath;

    @Override
    public void run(String... args) throws Exception {
        
        System.out.println("Loading CSV data using Spring Batch...");
        long totalStartTime = System.currentTimeMillis();
        
        // Execute Spring Batch job
        csvDataService.executeDataImport();
        
        long totalEndTime = System.currentTimeMillis();
        System.out.println("All CSV data loaded successfully! Total time: " + (totalEndTime - totalStartTime) + "ms");
    }
}
