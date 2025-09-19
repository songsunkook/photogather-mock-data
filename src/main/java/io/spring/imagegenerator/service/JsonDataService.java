package io.spring.imagegenerator.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

@Service
public class JsonDataService {

    @Autowired
    private JdbcBulkInsertService jdbcBulkInsertService;

    private final ObjectMapper objectMapper;
    private final AtomicLong idGenerator = new AtomicLong(1);
    
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
                                
                                if (processedCount % 10000 == 0) {
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
    
    private void processBatch(List<JsonSpaceData.SpaceData> batch) {
        long batchStartTime = System.currentTimeMillis();
        
        List<Space> spaces = new ArrayList<>();
        List<Host> hosts = new ArrayList<>();
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        List<HostKakao> hostKakaos = new ArrayList<>();
        List<Guest> guests = new ArrayList<>();
        List<SpaceContent> spaceContents = new ArrayList<>();
        List<Photo> photos = new ArrayList<>();
        
        // 1. Spaces 생성
        long spaceBuildStart = System.currentTimeMillis();
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
            space.setId(idGenerator.getAndIncrement());
            spaces.add(space);
        }
        long spaceBuildEnd = System.currentTimeMillis();
        
        // 2. Spaces bulk insert
        long spaceInsertStart = System.currentTimeMillis();
        jdbcBulkInsertService.bulkInsertSpaces(spaces);
        long spaceInsertEnd = System.currentTimeMillis();
        
        // 3. 관련 데이터 생성 및 저장
        long otherDataStart = System.currentTimeMillis();
        for (int i = 0; i < batch.size(); i++) {
            JsonSpaceData.SpaceData spaceData = batch.get(i);
            Space space = spaces.get(i);
            
            // Host 데이터 생성
            if (spaceData.getHost() != null) {
                Host host = createHost(spaceData.getHost());
                host.setId(idGenerator.getAndIncrement());
                hosts.add(host);
                
                // SpaceHostMap 생성
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    space.getId(),
                    host.getId(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
                );
                spaceHostMap.setId(idGenerator.getAndIncrement());
                spaceHostMaps.add(spaceHostMap);
                
                // HostKakao 생성
                if (spaceData.getHost().getKakao() != null) {
                    HostKakao hostKakao = new HostKakao(
                        host.getId(),
                        spaceData.getHost().getKakao().getUserId()
                    );
                    hostKakao.setId(idGenerator.getAndIncrement());
                    hostKakaos.add(hostKakao);
                }
            }
            
            // Guest 데이터 생성
            if (spaceData.getGuests() != null) {
                for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                    Guest guest = new Guest(
                        space.getId(),
                        guestData.getName(),
                        guestData.getCreatedAt(),
                        guestData.getUpdatedAt()
                    );
                    guest.setId(idGenerator.getAndIncrement());
                    guests.add(guest);
                    
                    // Photo 데이터 생성
                    if (guestData.getPhotos() != null) {
                        for (JsonSpaceData.PhotoData photoData : guestData.getPhotos()) {
                            Long spaceContentId = idGenerator.getAndIncrement();
                            
                            SpaceContent spaceContent = new SpaceContent(
                                SpaceContent.ContentType.valueOf(photoData.getContentType()),
                                space.getId(),
                                guest.getId()
                            );
                            spaceContent.setId(spaceContentId);
                            spaceContents.add(spaceContent);
                            
                            Photo photo = new Photo(
                                photoData.getOriginalName(),
                                photoData.getPath(),
                                photoData.getCapturedAt(), // nullable
                                photoData.getCapacity(),
                                photoData.getCreatedAt() != null ? photoData.getCreatedAt() : LocalDateTime.now()
                            );
                            photo.setId(spaceContentId); // Photo는 SpaceContent와 동일한 ID 사용
                            photos.add(photo);
                        }
                    }
                }
            }
        }
        long otherDataEnd = System.currentTimeMillis();
        
        // 4. 관련 데이터들을 bulk insert
        long bulkInsertStart = System.currentTimeMillis();
        if (!hosts.isEmpty()) {
            jdbcBulkInsertService.bulkInsertHosts(hosts);
        }
        if (!spaceHostMaps.isEmpty()) {
            jdbcBulkInsertService.bulkInsertSpaceHostMaps(spaceHostMaps);
        }
        if (!hostKakaos.isEmpty()) {
            jdbcBulkInsertService.bulkInsertHostKakaos(hostKakaos);
        }
        if (!guests.isEmpty()) {
            jdbcBulkInsertService.bulkInsertGuests(guests);
        }
        if (!spaceContents.isEmpty()) {
            jdbcBulkInsertService.bulkInsertSpaceContents(spaceContents);
        }
        if (!photos.isEmpty()) {
            jdbcBulkInsertService.bulkInsertPhotos(photos);
        }
        long bulkInsertEnd = System.currentTimeMillis();
        
        long batchEndTime = System.currentTimeMillis();
        long totalBatchTime = batchEndTime - batchStartTime;
        
        System.out.printf("Batch: %d spaces | Build: %dms, SpaceInsert: %dms, DataBuild: %dms, BulkInsert: %dms | Total: %dms | (%d hosts, %d guests, %d photos)\n", 
            spaces.size(),
            (spaceBuildEnd - spaceBuildStart),
            (spaceInsertEnd - spaceInsertStart),
            (otherDataEnd - otherDataStart),
            (bulkInsertEnd - bulkInsertStart),
            totalBatchTime,
            hosts.size(), guests.size(), photos.size());
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