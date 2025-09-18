package io.spring.imagegenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.spring.imagegenerator.service.JsonDataService;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private JsonDataService jsonDataService;
    
    @Value("${json.data.path}")
    private String jsonDataPath;

    @Override
    public void run(String... args) throws Exception {
        
        System.out.println("Loading JSON data...");
        long totalStartTime = System.currentTimeMillis();
        
        long startTime = System.currentTimeMillis();
        jsonDataService.loadSpaceFromJson(jsonDataPath + "/space_data.json");
        long endTime = System.currentTimeMillis();
        System.out.println("[100%] Loaded space data from JSON - " + (endTime - startTime) + "ms");
        
        long totalEndTime = System.currentTimeMillis();
        System.out.println("All JSON data loaded successfully! Total time: " + (totalEndTime - totalStartTime) + "ms");
    }
}
