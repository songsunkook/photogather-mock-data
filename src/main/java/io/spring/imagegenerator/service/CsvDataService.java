package io.spring.imagegenerator.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.spring.imagegenerator.entity.*;
import io.spring.imagegenerator.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
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
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1; // 헤더 제외
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                Space space = new Space(
                    record[1], // code
                    record[2].replace("\"", ""), // name
                    Integer.parseInt(record[3]), // valid_hours
                    LocalDateTime.parse(record[4], FORMATTER), // opened_at
                    Long.parseLong(record[5]), // max_capacity
                    Space.SpaceType.valueOf(record[6]), // type
                    LocalDateTime.parse(record[7], FORMATTER), // created_at
                    LocalDateTime.parse(record[8], FORMATTER) // updated_at
                );
                space.setId(Long.parseLong(record[0]));
                spaceRepository.save(space);
                
                // 10% 단위로 진행률 출력
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  Spaces: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load spaces from CSV", e);
        }
    }

    public void loadHostsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                Host host = new Host(
                    record[1].replace("\"", ""), // name
                    record[2], // picture_url
                    Boolean.parseBoolean(record[3]), // agreed_terms
                    LocalDateTime.parse(record[4], FORMATTER), // created_at
                    LocalDateTime.parse(record[5], FORMATTER) // updated_at
                );
                host.setId(Long.parseLong(record[0]));
                hostRepository.save(host);
                
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  Hosts: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load hosts from CSV", e);
        }
    }

    public void loadSpaceHostMapsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    Long.parseLong(record[1]), // space_id
                    Long.parseLong(record[2]), // host_id
                    LocalDateTime.parse(record[3], FORMATTER), // created_at
                    LocalDateTime.parse(record[4], FORMATTER) // updated_at
                );
                spaceHostMap.setId(Long.parseLong(record[0]));
                spaceHostMapRepository.save(spaceHostMap);
                
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  SpaceHostMaps: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space host maps from CSV", e);
        }
    }

    public void loadHostKakaosFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[1]), // host_id
                    record[2] // user_id
                );
                hostKakao.setId(Long.parseLong(record[0]));
                hostKakaoRepository.save(hostKakao);
                
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  HostKakaos: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load host kakaos from CSV", e);
        }
    }

    public void loadGuestsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                Guest guest = new Guest(
                    Long.parseLong(record[1]), // space_id
                    record[2].replace("\"", ""), // name
                    LocalDateTime.parse(record[3], FORMATTER), // created_at
                    LocalDateTime.parse(record[4], FORMATTER) // updated_at
                );
                guest.setId(Long.parseLong(record[0]));
                guestRepository.save(guest);
                
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  Guests: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load guests from CSV", e);
        }
    }

    public void loadSpaceContentsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[1]), // content_type
                    Long.parseLong(record[2]), // space_id
                    Long.parseLong(record[3]) // guest_id
                );
                spaceContent.setId(Long.parseLong(record[0]));
                spaceContentRepository.save(spaceContent);
                
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  SpaceContents: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space contents from CSV", e);
        }
    }

    public void loadPhotosFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                Photo photo = new Photo(
                    record[1].replace("\"", ""), // original_name
                    record[2].replace("\"", ""), // path
                    LocalDateTime.parse(record[3], FORMATTER), // captured_at
                    Long.parseLong(record[4]), // capacity
                    LocalDateTime.parse(record[5], FORMATTER) // created_at
                );
                photo.setId(Long.parseLong(record[0]));
                photoRepository.save(photo);
                
                int progress = (i * 100) / totalRecords;
                if (i % Math.max(1, totalRecords / 10) == 0 || i == totalRecords) {
                    System.out.printf("  Photos: %d/%d (%d%%)\n", i, totalRecords, progress);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load photos from CSV", e);
        }
    }
}