package io.spring.imagegenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.spring.imagegenerator.service.CsvDataService;
import io.spring.imagegenerator.service.JsonDataService;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private CsvDataService csvDataService;
    
    @Autowired
    private JsonDataService jsonDataService;
    
    @Value("${csv.data.path}")
    private String csvDataPath;
    
    @Value("${data.processing.mode:json}")
    private String processingMode;

    @Override
    public void run(String... args) throws Exception {
        
        long totalStartTime = System.currentTimeMillis();
        
        if ("json".equalsIgnoreCase(processingMode)) {
            System.out.println("Loading JSON data using Spring Batch...");
            jsonDataService.executeJsonDataImport();
            System.out.println("All JSON data loaded successfully!");
        } else {
            System.out.println("Loading CSV data using Spring Batch...");
            csvDataService.executeDataImport();
            System.out.println("All CSV data loaded successfully!");
        }
        
        long totalEndTime = System.currentTimeMillis();
        System.out.println("Total processing time: " + (totalEndTime - totalStartTime) + "ms");
    }
}
