package io.spring.imagegenerator.service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

@Service
@Transactional
public class CsvDataService {

    @Autowired
    private JdbcBulkInsertService jdbcBulkInsertService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void loadSpacesFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<Space> spaces = new ArrayList<>();
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
                spaces.add(space);
            }
            
            jdbcBulkInsertService.bulkInsertSpaces(spaces);
            System.out.printf("  Spaces: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load spaces from CSV", e);
        }
    }

    public void loadHostsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<Host> hosts = new ArrayList<>();
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
                hosts.add(host);
            }
            
            jdbcBulkInsertService.bulkInsertHosts(hosts);
            System.out.printf("  Hosts: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load hosts from CSV", e);
        }
    }

    public void loadSpaceHostMapsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    Long.parseLong(record[1]), // space_id
                    Long.parseLong(record[2]), // host_id
                    LocalDateTime.parse(record[3], FORMATTER), // created_at
                    LocalDateTime.parse(record[4], FORMATTER) // updated_at
                );
                spaceHostMap.setId(Long.parseLong(record[0]));
                spaceHostMaps.add(spaceHostMap);
            }
            
            jdbcBulkInsertService.bulkInsertSpaceHostMaps(spaceHostMaps);
            System.out.printf("  SpaceHostMaps: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space host maps from CSV", e);
        }
    }

    public void loadHostKakaosFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<HostKakao> hostKakaos = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[1]), // host_id
                    record[2] // user_id
                );
                hostKakao.setId(Long.parseLong(record[0]));
                hostKakaos.add(hostKakao);
            }
            
            jdbcBulkInsertService.bulkInsertHostKakaos(hostKakaos);
            System.out.printf("  HostKakaos: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load host kakaos from CSV", e);
        }
    }

    public void loadGuestsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<Guest> guests = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                Guest guest = new Guest(
                    Long.parseLong(record[1]), // space_id
                    record[2].replace("\"", ""), // name
                    LocalDateTime.parse(record[3], FORMATTER), // created_at
                    LocalDateTime.parse(record[4], FORMATTER) // updated_at
                );
                guest.setId(Long.parseLong(record[0]));
                guests.add(guest);
            }
            
            jdbcBulkInsertService.bulkInsertGuests(guests);
            System.out.printf("  Guests: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load guests from CSV", e);
        }
    }

    public void loadSpaceContentsFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<SpaceContent> spaceContents = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[1]), // content_type
                    Long.parseLong(record[2]), // space_id
                    Long.parseLong(record[3]) // guest_id
                );
                spaceContent.setId(Long.parseLong(record[0]));
                spaceContents.add(spaceContent);
            }
            
            jdbcBulkInsertService.bulkInsertSpaceContents(spaceContents);
            System.out.printf("  SpaceContents: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space contents from CSV", e);
        }
    }

    public void loadPhotosFromCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();
            int totalRecords = records.size() - 1;
            
            List<Photo> photos = new ArrayList<>();
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
                photos.add(photo);
            }
            
            jdbcBulkInsertService.bulkInsertPhotos(photos);
            System.out.printf("  Photos: %d/%d (100%%)\n", totalRecords, totalRecords);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load photos from CSV", e);
        }
    }
}
