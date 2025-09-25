package io.spring.imagegenerator.service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

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
public class CsvDataService {

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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${csv.file.path.spaces}")
    private String spacesFilePath;
    
    @Value("${csv.file.path.hosts}")
    private String hostsFilePath;
    
    @Value("${csv.file.path.hostKakaos}")
    private String hostKakaosFilePath;
    
    @Value("${csv.file.path.spaceHostMaps}")
    private String spaceHostMapsFilePath;
    
    @Value("${csv.file.path.guests}")
    private String guestsFilePath;
    
    @Value("${csv.file.path.spaceContents}")
    private String spaceContentsFilePath;
    
    @Value("${csv.file.path.photos}")
    private String photosFilePath;

    public void loadAllDataFromCsv() {
        long totalStartTime = System.currentTimeMillis();
        
        System.out.println("Starting cyclic batch processing of CSV data...");
        System.out.print("Cycle: ");
        
        try (CSVReader spacesReader = new CSVReader(new FileReader(spacesFilePath));
             CSVReader hostsReader = new CSVReader(new FileReader(hostsFilePath));
             CSVReader hostKakaosReader = new CSVReader(new FileReader(hostKakaosFilePath));
             CSVReader spaceHostMapsReader = new CSVReader(new FileReader(spaceHostMapsFilePath));
             CSVReader guestsReader = new CSVReader(new FileReader(guestsFilePath));
             CSVReader spaceContentsReader = new CSVReader(new FileReader(spaceContentsFilePath));
             CSVReader photosReader = new CSVReader(new FileReader(photosFilePath))) {
            
            // Skip headers
            spacesReader.readNext();
            hostsReader.readNext();
            hostKakaosReader.readNext();
            spaceHostMapsReader.readNext();
            guestsReader.readNext();
            spaceContentsReader.readNext();
            photosReader.readNext();
            
            boolean hasData = true;
            int cycleCount = 0;
            
            while (hasData) {
                cycleCount++;
                boolean cycleHasData = false;
                
                // Process spaces batch
                if (processBatch(spacesReader, "spaces", 100, cycleCount)) cycleHasData = true;
                
                // Process hosts batch
                if (processBatch(hostsReader, "hosts", 100, cycleCount)) cycleHasData = true;
                
                // Process host_kakaos batch
                if (processBatch(hostKakaosReader, "host_kakao", 100, cycleCount)) cycleHasData = true;
                
                // Process space_host_maps batch
                if (processBatch(spaceHostMapsReader, "space_host_map", 100, cycleCount)) cycleHasData = true;
                
                // Process guests batch
                if (processBatch(guestsReader, "guests", 1000, cycleCount)) cycleHasData = true;
                
                // Process space_contents batch
                if (processBatch(spaceContentsReader, "content", 20000, cycleCount)) cycleHasData = true;
                
                // Process photos batch
                if (processBatch(photosReader, "photos", 20000, cycleCount)) cycleHasData = true;
                
                hasData = cycleHasData;
            }
            
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load data from CSV files", e);
        }
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        
        System.out.printf(" | Total: %dms\n", totalTime);
    }
    
    private boolean processBatch(CSVReader reader, String tableName, int batchSize, int cycleCount) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        
        switch (tableName) {
            case "spaces":
                return processSpacesBatch(reader, batchSize, startTime);
            case "hosts":
                return processHostsBatch(reader, batchSize, startTime);
            case "host_kakao":
                return processHostKakaosBatch(reader, batchSize, startTime);
            case "space_host_map":
                return processSpaceHostMapsBatch(reader, batchSize, startTime);
            case "guests":
                return processGuestsBatch(reader, batchSize, startTime);
            case "content":
                return processSpaceContentsBatch(reader, batchSize, startTime);
            case "photos":
                return processPhotosBatch(reader, batchSize, startTime);
            default:
                return false;
        }
    }
    
    private boolean processSpacesBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<Space> spaces = new ArrayList<>();
        String[] record;
        int count = 0;
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            Space space = new Space(
                record[0], // code
                record[1].replace("\"", ""), // name
                Integer.parseInt(record[2]), // valid_hours
                LocalDateTime.parse(record[3], FORMATTER), // opened_at
                Long.parseLong(record[4]), // max_capacity
                Space.SpaceType.valueOf(record[5]), // type
                LocalDateTime.parse(record[6], FORMATTER), // created_at
                LocalDateTime.parse(record[7], FORMATTER) // updated_at
            );
            spaces.add(space);
        }
        
        if (!spaces.isEmpty()) {
            spaceRepository.saveAll(spaces);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d spaces (%dms), ", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private boolean processHostsBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<Host> hosts = new ArrayList<>();
        String[] record;
        int count = 0;
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            Host host = new Host(
                record[0].replace("\"", ""), // name
                record[1], // picture_url
                Boolean.parseBoolean(record[2]), // agreed_terms
                LocalDateTime.parse(record[3], FORMATTER), // created_at
                LocalDateTime.parse(record[4], FORMATTER) // updated_at
            );
            hosts.add(host);
        }
        
        if (!hosts.isEmpty()) {
            hostRepository.saveAll(hosts);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d hosts (%dms), ", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private boolean processHostKakaosBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<HostKakao> hostKakaos = new ArrayList<>();
        String[] record;
        int count = 0;
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            HostKakao hostKakao = new HostKakao(
                Long.parseLong(record[0]), // host_id
                record[1] // user_id
            );
            hostKakaos.add(hostKakao);
        }
        
        if (!hostKakaos.isEmpty()) {
            hostKakaoRepository.saveAll(hostKakaos);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d host_kakao (%dms), ", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private boolean processSpaceHostMapsBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        String[] record;
        int count = 0;
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            SpaceHostMap spaceHostMap = new SpaceHostMap(
                Long.parseLong(record[0]), // space_id
                Long.parseLong(record[1]), // host_id
                LocalDateTime.parse(record[2], FORMATTER), // created_at
                LocalDateTime.parse(record[3], FORMATTER) // updated_at
            );
            spaceHostMaps.add(spaceHostMap);
        }
        
        if (!spaceHostMaps.isEmpty()) {
            spaceHostMapRepository.saveAll(spaceHostMaps);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d space_host_map (%dms), ", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private boolean processGuestsBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<Guest> guests = new ArrayList<>();
        String[] record;
        int count = 0;
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            Guest guest = new Guest(
                Long.parseLong(record[0]), // space_id
                record[1].replace("\"", ""), // name
                LocalDateTime.parse(record[2], FORMATTER), // created_at
                LocalDateTime.parse(record[3], FORMATTER) // updated_at
            );
            guests.add(guest);
        }
        
        if (!guests.isEmpty()) {
            guestRepository.saveAll(guests);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d guests (%dms), ", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private boolean processSpaceContentsBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<SpaceContent> spaceContents = new ArrayList<>();
        String[] record;
        int count = 0;
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            SpaceContent spaceContent = new SpaceContent(
                SpaceContent.ContentType.valueOf(record[0]), // content_type
                Long.parseLong(record[1]), // space_id
                Long.parseLong(record[2]) // guest_id
            );
            spaceContents.add(spaceContent);
        }
        
        if (!spaceContents.isEmpty()) {
            spaceContentRepository.saveAll(spaceContents);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d content (%dms), ", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private boolean processPhotosBatch(CSVReader reader, int batchSize, long startTime) throws IOException, CsvException {
        List<Photo> photos = new ArrayList<>();
        String[] record;
        int count = 0;
        int photoIdCounter = getNextPhotoId();
        
        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;
            
            // Handle null captured_at (empty value in CSV)
            LocalDateTime capturedAt = null;
            if (record.length > 2 && !record[2].trim().isEmpty()) {
                capturedAt = LocalDateTime.parse(record[2], FORMATTER);
            }
            
            Photo photo = new Photo(
                record[0].replace("\"", ""), // original_name
                record[1].replace("\"", ""), // path
                capturedAt, // captured_at (can be null)
                Long.parseLong(record[3]), // capacity
                LocalDateTime.parse(record[4], FORMATTER) // created_at
            );
            photo.setId((long) photoIdCounter++);
            photos.add(photo);
        }
        
        if (!photos.isEmpty()) {
            photoRepository.saveAll(photos);
            updatePhotoIdCounter(photoIdCounter);
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d photos (%dms) \n", count, executionTime);
            return true;
        }
        
        return false;
    }
    
    private static int photoIdCounter = 1;
    
    private int getNextPhotoId() {
        return photoIdCounter;
    }
    
    private void updatePhotoIdCounter(int newValue) {
        photoIdCounter = newValue;
    }

    public void loadSpacesBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Space> spaces = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                Space space = new Space(
                    record[0], // code
                    record[1].replace("\"", ""), // name
                    Integer.parseInt(record[2]), // valid_hours
                    LocalDateTime.parse(record[3], FORMATTER), // opened_at
                    Long.parseLong(record[4]), // max_capacity
                    Space.SpaceType.valueOf(record[5]), // type
                    LocalDateTime.parse(record[6], FORMATTER), // created_at
                    LocalDateTime.parse(record[7], FORMATTER) // updated_at
                );
                spaces.add(space);
                
                if (spaces.size() >= batchSize) {
                    spaceRepository.saveAll(spaces);
                    spaces.clear();
                }
            }
            
            if (!spaces.isEmpty()) {
                spaceRepository.saveAll(spaces);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d spaces (%dms), ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load spaces from CSV", e);
        }
    }

    public void loadHostsBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Host> hosts = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                Host host = new Host(
                    record[0].replace("\"", ""), // name
                    record[1], // picture_url
                    Boolean.parseBoolean(record[2]), // agreed_terms
                    LocalDateTime.parse(record[3], FORMATTER), // created_at
                    LocalDateTime.parse(record[4], FORMATTER) // updated_at
                );
                hosts.add(host);
                
                if (hosts.size() >= batchSize) {
                    hostRepository.saveAll(hosts);
                    hosts.clear();
                }
            }
            
            if (!hosts.isEmpty()) {
                hostRepository.saveAll(hosts);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d hosts (%dms), ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load hosts from CSV", e);
        }
    }

    public void loadSpaceHostMapsBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    Long.parseLong(record[0]), // space_id
                    Long.parseLong(record[1]), // host_id
                    LocalDateTime.parse(record[2], FORMATTER), // created_at
                    LocalDateTime.parse(record[3], FORMATTER) // updated_at
                );
                spaceHostMaps.add(spaceHostMap);
                
                if (spaceHostMaps.size() >= batchSize) {
                    spaceHostMapRepository.saveAll(spaceHostMaps);
                    spaceHostMaps.clear();
                }
            }
            
            if (!spaceHostMaps.isEmpty()) {
                spaceHostMapRepository.saveAll(spaceHostMaps);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d space_host_map (%dms), ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space host maps from CSV", e);
        }
    }

    public void loadHostKakaosBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<HostKakao> hostKakaos = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[0]), // host_id
                    record[1] // user_id
                );
                hostKakaos.add(hostKakao);
                
                if (hostKakaos.size() >= batchSize) {
                    hostKakaoRepository.saveAll(hostKakaos);
                    hostKakaos.clear();
                }
            }
            
            if (!hostKakaos.isEmpty()) {
                hostKakaoRepository.saveAll(hostKakaos);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d host_kakao (%dms), ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load host kakaos from CSV", e);
        }
    }

    public void loadGuestsBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Guest> guests = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                Guest guest = new Guest(
                    Long.parseLong(record[0]), // space_id
                    record[1].replace("\"", ""), // name
                    LocalDateTime.parse(record[2], FORMATTER), // created_at
                    LocalDateTime.parse(record[3], FORMATTER) // updated_at
                );
                guests.add(guest);
                
                if (guests.size() >= batchSize) {
                    guestRepository.saveAll(guests);
                    guests.clear();
                }
            }
            
            if (!guests.isEmpty()) {
                guestRepository.saveAll(guests);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d guests (%dms), ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load guests from CSV", e);
        }
    }

    public void loadSpaceContentsBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<SpaceContent> spaceContents = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[0]), // content_type
                    Long.parseLong(record[1]), // space_id
                    Long.parseLong(record[2]) // guest_id
                );
                spaceContents.add(spaceContent);
                
                if (spaceContents.size() >= batchSize) {
                    spaceContentRepository.saveAll(spaceContents);
                    spaceContents.clear();
                }
            }
            
            if (!spaceContents.isEmpty()) {
                spaceContentRepository.saveAll(spaceContents);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d content (%dms), ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space contents from CSV", e);
        }
    }

    public void loadPhotosBatch(String filePath, int batchSize) {
        long startTime = System.currentTimeMillis();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Photo> photos = new ArrayList<>();
            String[] record;
            int totalProcessedCount = 0;
            
            while ((record = reader.readNext()) != null) {
                totalProcessedCount++;
                
                // Handle null captured_at (empty value in CSV)
                LocalDateTime capturedAt = null;
                if (record.length > 2 && !record[2].trim().isEmpty()) {
                    capturedAt = LocalDateTime.parse(record[2], FORMATTER);
                }
                
                Photo photo = new Photo(
                    record[0].replace("\"", ""), // original_name
                    record[1].replace("\"", ""), // path
                    capturedAt, // captured_at (can be null)
                    Long.parseLong(record[3]), // capacity
                    LocalDateTime.parse(record[4], FORMATTER) // created_at
                );
                photo.setId((long) totalProcessedCount); // Photo ID는 SpaceContent ID와 매칭되어야 함
                photos.add(photo);
                
                if (photos.size() >= batchSize) {
                    photoRepository.saveAll(photos);
                    photos.clear();
                }
            }
            
            if (!photos.isEmpty()) {
                photoRepository.saveAll(photos);
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.printf("%d photos (%dms) | Total: ", totalProcessedCount, executionTime);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load photos from CSV", e);
        }
    }
}
