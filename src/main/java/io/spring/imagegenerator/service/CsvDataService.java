package io.spring.imagegenerator.service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

    public void loadSpacesFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Space> spaces = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
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
                
                // Process in batches of 10000 to manage memory
                if (spaces.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  Spaces: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    spaceRepository.saveAll(spaces);
                    spaces.clear();
                }
            }
            
            // Process remaining records
            if (!spaces.isEmpty()) {
                batchCount++;
                System.out.printf("  Spaces: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                spaceRepository.saveAll(spaces);
            }
            
            System.out.printf("  Spaces: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load spaces from CSV", e);
        }
    }

    public void loadHostsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Host> hosts = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
                Host host = new Host(
                    record[0].replace("\"", ""), // name
                    record[1], // picture_url
                    Boolean.parseBoolean(record[2]), // agreed_terms
                    LocalDateTime.parse(record[3], FORMATTER), // created_at
                    LocalDateTime.parse(record[4], FORMATTER) // updated_at
                );
                hosts.add(host);
                
                // Process in batches of 10000 to manage memory
                if (hosts.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  Hosts: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    hostRepository.saveAll(hosts);
                    hosts.clear();
                }
            }
            
            // Process remaining records
            if (!hosts.isEmpty()) {
                batchCount++;
                System.out.printf("  Hosts: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                hostRepository.saveAll(hosts);
            }
            
            System.out.printf("  Hosts: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load hosts from CSV", e);
        }
    }

    public void loadSpaceHostMapsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    Long.parseLong(record[0]), // space_id
                    Long.parseLong(record[1]), // host_id
                    LocalDateTime.parse(record[2], FORMATTER), // created_at
                    LocalDateTime.parse(record[3], FORMATTER) // updated_at
                );
                spaceHostMaps.add(spaceHostMap);
                
                // Process in batches of 10000 to manage memory
                if (spaceHostMaps.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  SpaceHostMaps: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    spaceHostMapRepository.saveAll(spaceHostMaps);
                    spaceHostMaps.clear();
                }
            }
            
            // Process remaining records
            if (!spaceHostMaps.isEmpty()) {
                batchCount++;
                System.out.printf("  SpaceHostMaps: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                spaceHostMapRepository.saveAll(spaceHostMaps);
            }
            
            System.out.printf("  SpaceHostMaps: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space host maps from CSV", e);
        }
    }

    public void loadHostKakaosFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<HostKakao> hostKakaos = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[0]), // host_id
                    record[1] // user_id
                );
                hostKakaos.add(hostKakao);
                
                // Process in batches of 10000 to manage memory
                if (hostKakaos.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  HostKakaos: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    hostKakaoRepository.saveAll(hostKakaos);
                    hostKakaos.clear();
                }
            }
            
            // Process remaining records
            if (!hostKakaos.isEmpty()) {
                batchCount++;
                System.out.printf("  HostKakaos: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                hostKakaoRepository.saveAll(hostKakaos);
            }
            
            System.out.printf("  HostKakaos: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load host kakaos from CSV", e);
        }
    }

    public void loadGuestsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Guest> guests = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
                Guest guest = new Guest(
                    Long.parseLong(record[0]), // space_id
                    record[1].replace("\"", ""), // name
                    LocalDateTime.parse(record[2], FORMATTER), // created_at
                    LocalDateTime.parse(record[3], FORMATTER) // updated_at
                );
                guests.add(guest);
                
                // Process in batches of 10000 to manage memory
                if (guests.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  Guests: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    guestRepository.saveAll(guests);
                    guests.clear();
                }
            }
            
            // Process remaining records
            if (!guests.isEmpty()) {
                batchCount++;
                System.out.printf("  Guests: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                guestRepository.saveAll(guests);
            }
            
            System.out.printf("  Guests: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load guests from CSV", e);
        }
    }

    public void loadSpaceContentsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<SpaceContent> spaceContents = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[0]), // content_type
                    Long.parseLong(record[1]), // space_id
                    Long.parseLong(record[2]) // guest_id
                );
                spaceContents.add(spaceContent);
                
                // Process in batches of 10000 to manage memory
                if (spaceContents.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  SpaceContents: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    spaceContentRepository.saveAll(spaceContents);
                    spaceContents.clear();
                }
            }
            
            // Process remaining records
            if (!spaceContents.isEmpty()) {
                batchCount++;
                System.out.printf("  SpaceContents: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                spaceContentRepository.saveAll(spaceContents);
            }
            
            System.out.printf("  SpaceContents: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space contents from CSV", e);
        }
    }

    public void loadPhotosFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headerRow = reader.readNext(); // Skip header
            if (headerRow == null) return;
            
            List<Photo> photos = new ArrayList<>();
            String[] record;
            int recordCount = 0;
            int batchCount = 0;
            
            while ((record = reader.readNext()) != null) {
                recordCount++;
                
                Photo photo = new Photo(
                    record[0].replace("\"", ""), // original_name
                    record[1].replace("\"", ""), // path
                    LocalDateTime.parse(record[2], FORMATTER), // captured_at
                    Long.parseLong(record[3]), // capacity
                    LocalDateTime.parse(record[4], FORMATTER) // created_at
                );
                photos.add(photo);
                
                // Process in batches of 10000 to manage memory
                if (photos.size() >= 10000) {
                    batchCount++;
                    System.out.printf("  Photos: Processing batch %d (records: %d)\n", batchCount, recordCount);
                    photoRepository.saveAll(photos);
                    photos.clear();
                }
            }
            
            // Process remaining records
            if (!photos.isEmpty()) {
                batchCount++;
                System.out.printf("  Photos: Processing final batch %d (records: %d)\n", batchCount, recordCount);
                photoRepository.saveAll(photos);
            }
            
            System.out.printf("  Photos: %d records processed (100%%)\n", recordCount);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load photos from CSV", e);
        }
    }
}
