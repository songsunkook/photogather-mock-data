package io.spring.imagegenerator.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.spring.imagegenerator.dto.JsonSpaceData;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;
import io.spring.imagegenerator.repository.GuestRepository;
import io.spring.imagegenerator.repository.HostKakaoRepository;
import io.spring.imagegenerator.repository.HostRepository;
import io.spring.imagegenerator.repository.PhotoRepository;
import io.spring.imagegenerator.repository.SpaceContentRepository;
import io.spring.imagegenerator.repository.SpaceHostMapRepository;
import io.spring.imagegenerator.repository.SpaceRepository;

@Service
public class JsonDataService {

    @Autowired
    private SpaceRepository spaceRepository;
    
    @Autowired
    private HostRepository hostRepository;
    
    @Autowired
    private SpaceHostMapRepository spaceHostMapRepository;
    
    @Autowired
    private HostKakaoRepository hostKakaoRepository;
    
    @Autowired
    private GuestRepository guestRepository;
    
    @Autowired
    private SpaceContentRepository spaceContentRepository;
    
    @Autowired
    private PhotoRepository photoRepository;

    private final ObjectMapper objectMapper;
    
    public JsonDataService() {
        this.objectMapper = new ObjectMapper();
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        
        this.objectMapper.registerModule(javaTimeModule);
    }

    public void loadSpaceFromJson(String filePath) {
        long totalStartTime = System.currentTimeMillis();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             JsonParser parser = new JsonFactory().createParser(fis)) {
            
            int processedCount = 0;
            int batchSize = 100; // 배치 사이즈 설정
            List<JsonSpaceData.SpaceData> batch = new ArrayList<>();
            
            System.out.printf("Starting to process JSON file: %s\n", filePath);
            
            if (parser.nextToken() == JsonToken.START_OBJECT) {
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();
                    
                    if ("space".equals(fieldName)) {
                        JsonSpaceData.SpaceData spaceData = objectMapper.readValue(parser, JsonSpaceData.SpaceData.class);
                        batch.add(spaceData);
                        processedCount++;
                        
                        if (batch.size() >= batchSize) {
                            processBatch(batch);
                            batch.clear();
                            System.gc(); // 메모리 정리
                        }
                    } else if ("spaces".equals(fieldName) && parser.getCurrentToken() == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            JsonSpaceData.SpaceData spaceData = objectMapper.readValue(parser, JsonSpaceData.SpaceData.class);
                            batch.add(spaceData);
                            processedCount++;
                            
                            if (batch.size() >= batchSize) {
                                processBatch(batch);
                                batch.clear();
                                System.gc(); // 메모리 정리
                                
                                if (processedCount % 1000 == 0) {
                                    long currentTime = System.currentTimeMillis();
                                    long elapsedTime = currentTime - totalStartTime;
                                    double avgTimePerSpace = (double) elapsedTime / processedCount;
                                    System.out.printf("Progress: %d spaces processed in %d ms (avg: %.2f ms/space)\n", 
                                        processedCount, elapsedTime, avgTimePerSpace);
                                }
                            }
                        }
                    }
                }
            }
            
            // 남은 배치 처리
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
            
            long totalEndTime = System.currentTimeMillis();
            long totalTime = totalEndTime - totalStartTime;
            double avgTimePerSpace = processedCount > 0 ? (double) totalTime / processedCount : 0;
            
            System.out.printf("=== PROCESSING COMPLETE ===\n");
            System.out.printf("Total spaces processed: %d\n", processedCount);
            System.out.printf("Total time: %d ms (%.2f seconds)\n", totalTime, totalTime / 1000.0);
            System.out.printf("Average time per space: %.2f ms\n", avgTimePerSpace);
            System.out.printf("Processing rate: %.2f spaces/second\n", processedCount > 0 ? (processedCount * 1000.0 / totalTime) : 0);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load space from JSON", e);
        }
    }
    
    @Transactional
    private void processBatch(List<JsonSpaceData.SpaceData> batch) {
        long batchStartTime = System.currentTimeMillis();
        
        List<Space> spaces = new ArrayList<>();
        List<Host> hosts = new ArrayList<>();
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        List<HostKakao> hostKakaos = new ArrayList<>();
        List<Guest> guests = new ArrayList<>();
        List<SpaceContent> spaceContents = new ArrayList<>();
        int photoCount = 0;
        
        for (JsonSpaceData.SpaceData spaceData : batch) {
            Space space = new Space(
                spaceData.getCode(),
                spaceData.getName(),
                spaceData.getValidHours(),
                spaceData.getOpenedAt(),
                spaceData.getMaxCapacity(),
                Space.SpaceType.valueOf(spaceData.getType()),
                spaceData.getCreatedAt(),
                spaceData.getUpdatedAt()
            );
            spaces.add(space);
        }
        
        // 1. Spaces 저장
        long spaceSaveStart = System.currentTimeMillis();
        List<Space> savedSpaces = spaceRepository.saveAll(spaces);
        long spaceSaveEnd = System.currentTimeMillis();
        
        // 2. 각 Space에 대해 관련 데이터 처리
        for (int i = 0; i < batch.size(); i++) {
            JsonSpaceData.SpaceData spaceData = batch.get(i);
            Space space = savedSpaces.get(i);
            
            if (spaceData.getHost() != null) {
                Host host = createHost(spaceData.getHost());
                hosts.add(host);
            }
        }
        
        // 3. Hosts 저장
        long hostSaveStart = System.currentTimeMillis();
        long hostSaveEnd = hostSaveStart;
        if (!hosts.isEmpty()) {
            List<Host> savedHosts = hostRepository.saveAll(hosts);
            hostSaveEnd = System.currentTimeMillis();
            
            // 4. SpaceHostMaps 생성
            int hostIndex = 0;
            for (int i = 0; i < batch.size(); i++) {
                JsonSpaceData.SpaceData spaceData = batch.get(i);
                Space space = savedSpaces.get(i);
                
                if (spaceData.getHost() != null) {
                    Host host = savedHosts.get(hostIndex);
                    SpaceHostMap spaceHostMap = new SpaceHostMap(
                        space.getId(),
                        host.getId(),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                    );
                    spaceHostMaps.add(spaceHostMap);
                    
                    if (spaceData.getHost().getKakao() != null) {
                        HostKakao hostKakao = new HostKakao(
                            host.getId(),
                            spaceData.getHost().getKakao().getUserId()
                        );
                        hostKakaos.add(hostKakao);
                    }
                    hostIndex++;
                }
            }
            
            // 5. SpaceHostMaps와 HostKakaos 저장
            if (!spaceHostMaps.isEmpty()) {
                spaceHostMapRepository.saveAll(spaceHostMaps);
            }
            if (!hostKakaos.isEmpty()) {
                hostKakaoRepository.saveAll(hostKakaos);
            }
        }
        
        // 6. Guests, SpaceContents, Photos 처리
        for (int i = 0; i < batch.size(); i++) {
            JsonSpaceData.SpaceData spaceData = batch.get(i);
            Space space = savedSpaces.get(i);
            
            if (spaceData.getGuests() != null) {
                for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                    Guest guest = new Guest(
                        space.getId(),
                        guestData.getName(),
                        guestData.getCreatedAt(),
                        guestData.getUpdatedAt()
                    );
                    guests.add(guest);
                }
            }
        }
        
        // 7. Guests 저장 후 Photos 처리
        long guestSaveStart = System.currentTimeMillis();
        long guestSaveEnd = guestSaveStart;
        long spaceContentSaveStart = System.currentTimeMillis();
        long spaceContentSaveEnd = spaceContentSaveStart;
        long photoSaveStart = System.currentTimeMillis();
        long photoSaveEnd = photoSaveStart;
        
        if (!guests.isEmpty()) {
            List<Guest> savedGuests = guestRepository.saveAll(guests);
            guestSaveEnd = System.currentTimeMillis();
            
            int guestIndex = 0;
            for (int i = 0; i < batch.size(); i++) {
                JsonSpaceData.SpaceData spaceData = batch.get(i);
                Space space = savedSpaces.get(i);
                
                if (spaceData.getGuests() != null) {
                    for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                        Guest guest = savedGuests.get(guestIndex);
                        
                        if (guestData.getPhotos() != null) {
                            for (JsonSpaceData.PhotoData photoData : guestData.getPhotos()) {
                                SpaceContent spaceContent = new SpaceContent(
                                    SpaceContent.ContentType.valueOf(photoData.getContentType()),
                                    space.getId(),
                                    guest.getId()
                                );
                                spaceContents.add(spaceContent);
                            }
                        }
                        guestIndex++;
                    }
                }
            }
            
            // 8. SpaceContents 저장 후 Photos 개별 저장
            if (!spaceContents.isEmpty()) {
                spaceContentSaveStart = System.currentTimeMillis();
                List<SpaceContent> savedSpaceContents = spaceContentRepository.saveAll(spaceContents);
                spaceContentSaveEnd = System.currentTimeMillis();
                
                photoSaveStart = System.currentTimeMillis();
                
                int spaceContentIndex = 0;
                guestIndex = 0;
                
                for (int i = 0; i < batch.size(); i++) {
                    JsonSpaceData.SpaceData spaceData = batch.get(i);
                    
                    if (spaceData.getGuests() != null) {
                        for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                            if (guestData.getPhotos() != null) {
                                for (JsonSpaceData.PhotoData photoData : guestData.getPhotos()) {
                                    SpaceContent spaceContent = savedSpaceContents.get(spaceContentIndex);
                                    
                                    Photo photo = new Photo(
                                        photoData.getOriginalName(),
                                        photoData.getPath(),
                                        photoData.getCapturedAt(),
                                        photoData.getCapacity(),
                                        photoData.getCreatedAt()
                                    );
                                    // ID만 직접 설정 (SpaceContent 객체 참조 없이)
                                    photo.setId(spaceContent.getId());
                                    photoRepository.save(photo); // 개별 저장
                                    photoCount++;
                                    spaceContentIndex++;
                                }
                            }
                            guestIndex++;
                        }
                    }
                }
                photoSaveEnd = System.currentTimeMillis();
            }
        }
        
        long batchEndTime = System.currentTimeMillis();
        long totalBatchTime = batchEndTime - batchStartTime;
        
        System.out.printf("Batch: %d spaces (%dms), %d hosts (%dms), %d guests (%dms), %d content (%dms), %d photos (%dms) | Total: %dms\n", 
            spaces.size(), (spaceSaveEnd - spaceSaveStart), 
            hosts.size(), (hostSaveEnd - hostSaveStart), 
            guests.size(), (guestSaveEnd - guestSaveStart),
            spaceContents.size(), (spaceContentSaveEnd - spaceContentSaveStart),
            photoCount, (photoSaveEnd - photoSaveStart),
            totalBatchTime);
    }
    
    private Host createHost(JsonSpaceData.HostData hostData) {
        return new Host(
            hostData.getName(),
            hostData.getPictureUrl(),
            hostData.getAgreedTerms(),
            hostData.getCreatedAt(),
            hostData.getUpdatedAt()
        );
    }
}
