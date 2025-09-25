package io.spring.imagegenerator.batch.step;

import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;
import jakarta.annotation.PostConstruct;

@Component
public class CyclicBatchStep implements Tasklet {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private static long spaceIdCounter = 1;
    private static long hostIdCounter = 1;
    private static long hostKakaoIdCounter = 1;
    private static long spaceHostMapIdCounter = 1;
    private static long guestIdCounter = 1;
    private static long spaceContentIdCounter = 1;
    private static long photoIdCounter = 1;

    @PostConstruct
    public void initializeIdCounters() {
        try {
            Long maxSpaceId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM space", Long.class);
            Long maxHostId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM host", Long.class);
            Long maxHostKakaoId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM host_kakao", Long.class);
            Long maxSpaceHostMapId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM space_host_map", Long.class);
            Long maxGuestId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM guest", Long.class);
            Long maxSpaceContentId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM space_content", Long.class);
            Long maxPhotoId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM photo", Long.class);

            spaceIdCounter = maxSpaceId + 1;
            hostIdCounter = maxHostId + 1;
            hostKakaoIdCounter = maxHostKakaoId + 1;
            spaceHostMapIdCounter = maxSpaceHostMapId + 1;
            guestIdCounter = maxGuestId + 1;
            spaceContentIdCounter = maxSpaceContentId + 1;
            photoIdCounter = maxPhotoId + 1;

            System.out.println("Spring Batch - Initialized ID counters from database:");
            System.out.printf("  Space: %d, Host: %d, HostKakao: %d, SpaceHostMap: %d%n",
                            spaceIdCounter, hostIdCounter, hostKakaoIdCounter, spaceHostMapIdCounter);
            System.out.printf("  Guest: %d, SpaceContent: %d, Photo: %d%n",
                            guestIdCounter, spaceContentIdCounter, photoIdCounter);
        } catch (Exception e) {
            System.out.println("Failed to initialize ID counters, using default values: " + e.getMessage());
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long totalStartTime = System.currentTimeMillis();

        System.out.println("Starting Spring Batch cyclic processing of CSV data...");
        System.out.print("Spring Batch Cycle: ");

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
                long cycleStartTime = System.currentTimeMillis();
                boolean cycleHasData = false;

                System.out.printf("\nCycle %d: ", cycleCount);

                // Process each table in the cycle
                if (processSpacesBatch(spacesReader, 100)) cycleHasData = true;
                if (processHostsBatch(hostsReader, 100)) cycleHasData = true;
                if (processSpaceHostMapsBatch(spaceHostMapsReader, 100)) cycleHasData = true;
                if (processHostKakaosBatch(hostKakaosReader, 100)) cycleHasData = true;
                if (processGuestsBatch(guestsReader, 1000)) cycleHasData = true;
                if (processSpaceContentsBatch(spaceContentsReader, 20000)) cycleHasData = true;
                if (processPhotosBatch(photosReader, 20000)) cycleHasData = true;

                long cycleEndTime = System.currentTimeMillis();
                long cycleTime = cycleEndTime - cycleStartTime;
                System.out.printf(" | Cycle Total: %dms", cycleTime);

                hasData = cycleHasData;
            }

        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load data from CSV files using Spring Batch", e);
        }

        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;

        System.out.printf("\n | Total Processing Time: %dms\n", totalTime);

        return RepeatStatus.FINISHED;
    }

    private boolean processSpacesBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<Space> spaces = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = spaceIdCounter;

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
            space.setId(idCounter++);
            spaces.add(space);
        }

        if (!spaces.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Space space = spaces.get(i);
                        ps.setLong(1, space.getId());
                        ps.setString(2, space.getCode());
                        ps.setString(3, space.getName());
                        ps.setInt(4, space.getValidHours());
                        ps.setTimestamp(5, Timestamp.valueOf(space.getOpenedAt()));
                        ps.setLong(6, space.getMaxCapacity());
                        ps.setString(7, space.getType().name());
                        ps.setTimestamp(8, Timestamp.valueOf(space.getCreatedAt()));
                        ps.setTimestamp(9, Timestamp.valueOf(space.getUpdatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return spaces.size();
                    }
                }
            );
            spaceIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d spaces (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }

    private boolean processHostsBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<Host> hosts = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = hostIdCounter;

        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;

            Host host = new Host(
                record[0].replace("\"", ""), // name
                record[1], // picture_url
                Boolean.parseBoolean(record[2]), // agreed_terms
                LocalDateTime.parse(record[3], FORMATTER), // created_at
                LocalDateTime.parse(record[4], FORMATTER) // updated_at
            );
            host.setId(idCounter++);
            hosts.add(host);
        }

        if (!hosts.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Host host = hosts.get(i);
                        ps.setLong(1, host.getId());
                        ps.setString(2, host.getName());
                        ps.setString(3, host.getPictureUrl());
                        ps.setBoolean(4, host.getAgreedTerms());
                        ps.setTimestamp(5, Timestamp.valueOf(host.getCreatedAt()));
                        ps.setTimestamp(6, Timestamp.valueOf(host.getUpdatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return hosts.size();
                    }
                }
            );
            hostIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d hosts (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }

    private boolean processSpaceHostMapsBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = spaceHostMapIdCounter;

        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;

            SpaceHostMap spaceHostMap = new SpaceHostMap(
                Long.parseLong(record[0]), // space_id
                Long.parseLong(record[1]), // host_id
                LocalDateTime.parse(record[2], FORMATTER), // created_at
                LocalDateTime.parse(record[3], FORMATTER) // updated_at
            );
            spaceHostMap.setId(idCounter++);
            spaceHostMaps.add(spaceHostMap);
        }

        if (!spaceHostMaps.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        SpaceHostMap spaceHostMap = spaceHostMaps.get(i);
                        ps.setLong(1, spaceHostMap.getId());
                        ps.setLong(2, spaceHostMap.getSpaceId());
                        ps.setLong(3, spaceHostMap.getHostId());
                        ps.setTimestamp(4, Timestamp.valueOf(spaceHostMap.getCreatedAt()));
                        ps.setTimestamp(5, Timestamp.valueOf(spaceHostMap.getUpdatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return spaceHostMaps.size();
                    }
                }
            );
            spaceHostMapIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d space_host_map (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }

    private boolean processHostKakaosBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<HostKakao> hostKakaos = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = hostKakaoIdCounter;

        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;

            HostKakao hostKakao = new HostKakao(
                Long.parseLong(record[0]), // host_id
                record[1] // user_id
            );
            hostKakao.setId(idCounter++);
            hostKakaos.add(hostKakao);
        }

        if (!hostKakaos.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        HostKakao hostKakao = hostKakaos.get(i);
                        ps.setLong(1, hostKakao.getId());
                        ps.setLong(2, hostKakao.getHostId());
                        ps.setString(3, hostKakao.getUserId());
                    }

                    @Override
                    public int getBatchSize() {
                        return hostKakaos.size();
                    }
                }
            );
            hostKakaoIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d host_kakao (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }

    private boolean processGuestsBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<Guest> guests = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = guestIdCounter;

        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;

            Guest guest = new Guest(
                Long.parseLong(record[0]), // space_id
                record[1].replace("\"", ""), // name
                LocalDateTime.parse(record[2], FORMATTER), // created_at
                LocalDateTime.parse(record[3], FORMATTER) // updated_at
            );
            guest.setId(idCounter++);
            guests.add(guest);
        }

        if (!guests.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Guest guest = guests.get(i);
                        ps.setLong(1, guest.getId());
                        ps.setLong(2, guest.getSpaceId());
                        ps.setString(3, guest.getName());
                        ps.setTimestamp(4, Timestamp.valueOf(guest.getCreatedAt()));
                        ps.setTimestamp(5, Timestamp.valueOf(guest.getUpdatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return guests.size();
                    }
                }
            );
            guestIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d guests (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }

    private boolean processSpaceContentsBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<SpaceContent> spaceContents = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = spaceContentIdCounter;

        while (count < batchSize && (record = reader.readNext()) != null) {
            count++;

            SpaceContent spaceContent = new SpaceContent(
                SpaceContent.ContentType.valueOf(record[0]), // content_type
                Long.parseLong(record[1]), // space_id
                Long.parseLong(record[2]) // guest_id
            );
            spaceContent.setId(idCounter++);
            spaceContents.add(spaceContent);
        }

        if (!spaceContents.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        SpaceContent spaceContent = spaceContents.get(i);
                        ps.setLong(1, spaceContent.getId());
                        ps.setString(2, spaceContent.getContentType().name());
                        ps.setLong(3, spaceContent.getSpaceId());
                        if (spaceContent.getGuestId() != null) {
                            ps.setLong(4, spaceContent.getGuestId());
                        } else {
                            ps.setNull(4, java.sql.Types.BIGINT);
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        return spaceContents.size();
                    }
                }
            );
            spaceContentIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d content (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }

    private boolean processPhotosBatch(CSVReader reader, int batchSize) throws IOException, CsvException {
        long startTime = System.currentTimeMillis();
        List<Photo> photos = new ArrayList<>();
        String[] record;
        int count = 0;
        long idCounter = photoIdCounter;

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
            photo.setId(idCounter++);
            photos.add(photo);
        }

        if (!photos.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Photo photo = photos.get(i);
                        ps.setLong(1, photo.getId());
                        ps.setString(2, photo.getOriginalName());
                        ps.setString(3, photo.getPath());
                        if (photo.getCapturedAt() != null) {
                            ps.setTimestamp(4, Timestamp.valueOf(photo.getCapturedAt()));
                        } else {
                            ps.setNull(4, java.sql.Types.TIMESTAMP);
                        }
                        ps.setLong(5, photo.getCapacity());
                        ps.setTimestamp(6, Timestamp.valueOf(photo.getCreatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return photos.size();
                    }
                }
            );
            photoIdCounter = idCounter;
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.printf("%d photos (%dms), ", count, executionTime);
            return true;
        }

        return false;
    }
}
