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
        
        System.out.println("Loading CSV data...");
        long totalStartTime = System.currentTimeMillis();
        
        // 1. Spaces (10%)
        long startTime = System.currentTimeMillis();
        csvDataService.loadSpacesFromCsv(csvDataPath + "/1_space.csv");
        long endTime = System.currentTimeMillis();
        System.out.println("[10%] Loaded spaces - " + (endTime - startTime) + "ms");
        
        // 2. Hosts (20%)
        startTime = System.currentTimeMillis();
        csvDataService.loadHostsFromCsv(csvDataPath + "/2_host.csv");
        endTime = System.currentTimeMillis();
        System.out.println("[20%] Loaded hosts - " + (endTime - startTime) + "ms");
        
        // 3. Space Host Maps (30%)
        startTime = System.currentTimeMillis();
        csvDataService.loadSpaceHostMapsFromCsv(csvDataPath + "/3_space_host_map.csv");
        endTime = System.currentTimeMillis();
        System.out.println("[30%] Loaded space host maps - " + (endTime - startTime) + "ms");
        
        // 4. Host Kakaos (50%)
        startTime = System.currentTimeMillis();
        csvDataService.loadHostKakaosFromCsv(csvDataPath + "/4_host_kakao.csv");
        endTime = System.currentTimeMillis();
        System.out.println("[50%] Loaded host kakaos - " + (endTime - startTime) + "ms");
        
        // 5. Guests (70%)
        startTime = System.currentTimeMillis();
        csvDataService.loadGuestsFromCsv(csvDataPath + "/5_guest.csv");
        endTime = System.currentTimeMillis();
        System.out.println("[70%] Loaded guests - " + (endTime - startTime) + "ms");
        
        // 6. Space Contents (80%)
        startTime = System.currentTimeMillis();
        csvDataService.loadSpaceContentsFromCsv(csvDataPath + "/6_space_content.csv");
        endTime = System.currentTimeMillis();
        System.out.println("[80%] Loaded space contents - " + (endTime - startTime) + "ms");
        
        // 7. Photos (100%)
        startTime = System.currentTimeMillis();
        csvDataService.loadPhotosFromCsv(csvDataPath + "/7_photo.csv");
        endTime = System.currentTimeMillis();
        System.out.println("[100%] Loaded photos - " + (endTime - startTime) + "ms");
        
        long totalEndTime = System.currentTimeMillis();
        System.out.println("All CSV data loaded successfully! Total time: " + (totalEndTime - totalStartTime) + "ms");
    }
}
