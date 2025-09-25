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
public class CyclicTableStep {

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

    // CSV 리더의 현재 위치를 추적
    private static long spacesProcessed = 0;
    private static long hostsProcessed = 0;
    private static long hostKakaosProcessed = 0;
    private static long spaceHostMapsProcessed = 0;
    private static long guestsProcessed = 0;
    private static long spaceContentsProcessed = 0;
    private static long photosProcessed = 0;
    
    // 각 CSV 파일의 전체 라인 수 캐싱
    private static Long spacesTotalLines = null;
    private static Long hostsTotalLines = null;
    private static Long hostKakaosTotalLines = null;
    private static Long spaceHostMapsTotalLines = null;
    private static Long guestsTotalLines = null;
    private static Long spaceContentsTotalLines = null;
    private static Long photosTotalLines = null;

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

            // 이미 처리된 레코드 수 계산
            spacesProcessed = maxSpaceId;
            hostsProcessed = maxHostId;
            hostKakaosProcessed = maxHostKakaoId;
            spaceHostMapsProcessed = maxSpaceHostMapId;
            guestsProcessed = maxGuestId;
            spaceContentsProcessed = maxSpaceContentId;
            photosProcessed = maxPhotoId;

            System.out.println("Spring Batch Cyclic - Initialized counters:");
            System.out.printf("  Space: %d (processed: %d), Host: %d (processed: %d)%n",
                            spaceIdCounter, spacesProcessed, hostIdCounter, hostsProcessed);
            System.out.printf("  HostKakao: %d (processed: %d), SpaceHostMap: %d (processed: %d)%n",
                            hostKakaoIdCounter, hostKakaosProcessed, spaceHostMapIdCounter, spaceHostMapsProcessed);
            System.out.printf("  Guest: %d (processed: %d), SpaceContent: %d (processed: %d), Photo: %d (processed: %d)%n",
                            guestIdCounter, guestsProcessed, spaceContentIdCounter, spaceContentsProcessed, photoIdCounter, photosProcessed);
                            
            // Initialize total line counts if not already cached
            initializeTotalLineCounts();
        } catch (Exception e) {
            System.out.println("Failed to initialize counters: " + e.getMessage());
        }
    }
    
    private void initializeTotalLineCounts() {
        if (spacesTotalLines == null) {
            spacesTotalLines = countCsvLines(spacesFilePath);
            hostsTotalLines = countCsvLines(hostsFilePath);
            hostKakaosTotalLines = countCsvLines(hostKakaosFilePath);
            spaceHostMapsTotalLines = countCsvLines(spaceHostMapsFilePath);
            guestsTotalLines = countCsvLines(guestsFilePath);
            spaceContentsTotalLines = countCsvLines(spaceContentsFilePath);
            photosTotalLines = countCsvLines(photosFilePath);
            
            System.out.println("CSV file line counts (excluding header):");
            System.out.printf("  Spaces: %d, Hosts: %d, HostKakaos: %d, SpaceHostMaps: %d%n",
                            spacesTotalLines, hostsTotalLines, hostKakaosTotalLines, spaceHostMapsTotalLines);
            System.out.printf("  Guests: %d, SpaceContents: %d, Photos: %d%n",
                            guestsTotalLines, spaceContentsTotalLines, photosTotalLines);
        }
    }
    
    private long countCsvLines(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            reader.readNext(); // Skip header
            long count = 0;
            while (reader.readNext() != null) {
                count++;
            }
            return count;
        } catch (Exception e) {
            System.out.printf("Error counting lines in %s: %s%n", filePath, e.getMessage());
            return 0;
        }
    }
    
    public static boolean isAllDataProcessed() {
        // Check if all CSV files have been completely processed
        return spacesTotalLines != null && 
               spacesProcessed >= spacesTotalLines &&
               hostsProcessed >= hostsTotalLines &&
               hostKakaosProcessed >= hostKakaosTotalLines &&
               spaceHostMapsProcessed >= spaceHostMapsTotalLines &&
               guestsProcessed >= guestsTotalLines &&
               spaceContentsProcessed >= spaceContentsTotalLines &&
               photosProcessed >= photosTotalLines;
    }
    
    public static void printProgressStatus() {
        if (spacesTotalLines != null) {
            System.out.println("CSV Processing Progress:");
            System.out.printf("  Spaces: %d/%d (%.1f%%)%n", spacesProcessed, spacesTotalLines, 
                            (spacesProcessed * 100.0) / spacesTotalLines);
            System.out.printf("  Hosts: %d/%d (%.1f%%)%n", hostsProcessed, hostsTotalLines,
                            (hostsProcessed * 100.0) / hostsTotalLines);
            System.out.printf("  HostKakaos: %d/%d (%.1f%%)%n", hostKakaosProcessed, hostKakaosTotalLines,
                            (hostKakaosProcessed * 100.0) / hostKakaosTotalLines);
            System.out.printf("  SpaceHostMaps: %d/%d (%.1f%%)%n", spaceHostMapsProcessed, spaceHostMapsTotalLines,
                            (spaceHostMapsProcessed * 100.0) / spaceHostMapsTotalLines);
            System.out.printf("  Guests: %d/%d (%.1f%%)%n", guestsProcessed, guestsTotalLines,
                            (guestsProcessed * 100.0) / guestsTotalLines);
            System.out.printf("  SpaceContents: %d/%d (%.1f%%)%n", spaceContentsProcessed, spaceContentsTotalLines,
                            (spaceContentsProcessed * 100.0) / spaceContentsTotalLines);
            System.out.printf("  Photos: %d/%d (%.1f%%)%n", photosProcessed, photosTotalLines,
                            (photosProcessed * 100.0) / photosTotalLines);
        }
    }

    // Spaces Step
    public static class SpacesStep implements Tasklet {
        private final CyclicTableStep parent;

        public SpacesStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processSpaces();
        }
    }

    // Hosts Step
    public static class HostsStep implements Tasklet {
        private final CyclicTableStep parent;

        public HostsStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processHosts();
        }
    }

    // SpaceHostMaps Step
    public static class SpaceHostMapsStep implements Tasklet {
        private final CyclicTableStep parent;

        public SpaceHostMapsStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processSpaceHostMaps();
        }
    }

    // HostKakaos Step
    public static class HostKakaosStep implements Tasklet {
        private final CyclicTableStep parent;

        public HostKakaosStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processHostKakaos();
        }
    }

    // Guests Step
    public static class GuestsStep implements Tasklet {
        private final CyclicTableStep parent;

        public GuestsStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processGuests();
        }
    }

    // SpaceContents Step
    public static class SpaceContentsStep implements Tasklet {
        private final CyclicTableStep parent;

        public SpaceContentsStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processSpaceContents();
        }
    }

    // Photos Step
    public static class PhotosStep implements Tasklet {
        private final CyclicTableStep parent;

        public PhotosStep(CyclicTableStep parent) {
            this.parent = parent;
        }

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            return parent.processPhotos();
        }
    }

    private RepeatStatus processSpaces() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(spacesFilePath))) {
            reader.readNext(); // Skip header

            // 이미 처리된 라인만큼 스킵
            for (long i = 0; i < spacesProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<Space> spaces = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = spaceIdCounter;

            while (count < 100 && (record = reader.readNext()) != null) {
                count++;
                spacesProcessed++;

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
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < spaces.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?)");

                    Space space = spaces.get(i);
                    params.add(space.getId());
                    params.add(space.getCode());
                    params.add(space.getName());
                    params.add(space.getValidHours());
                    params.add(Timestamp.valueOf(space.getOpenedAt()));
                    params.add(space.getMaxCapacity());
                    params.add(space.getType().name());
                    params.add(Timestamp.valueOf(space.getCreatedAt()));
                    params.add(Timestamp.valueOf(space.getUpdatedAt()));
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                spaceIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("Spaces Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("Spaces Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }

    private RepeatStatus processHosts() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(hostsFilePath))) {
            reader.readNext(); // Skip header

            // 이미 처리된 라인만큼 스킵
            for (long i = 0; i < hostsProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<Host> hosts = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = hostIdCounter;

            while (count < 100 && (record = reader.readNext()) != null) {
                count++;
                hostsProcessed++;

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
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < hosts.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?, ?, ?, ?)");

                    Host host = hosts.get(i);
                    params.add(host.getId());
                    params.add(host.getName());
                    params.add(host.getPictureUrl());
                    params.add(host.getAgreedTerms());
                    params.add(Timestamp.valueOf(host.getCreatedAt()));
                    params.add(Timestamp.valueOf(host.getUpdatedAt()));
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                hostIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("Hosts Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("Hosts Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }

    private RepeatStatus processSpaceHostMaps() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(spaceHostMapsFilePath))) {
            reader.readNext(); // Skip header

            for (long i = 0; i < spaceHostMapsProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = spaceHostMapIdCounter;

            while (count < 100 && (record = reader.readNext()) != null) {
                count++;
                spaceHostMapsProcessed++;

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
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < spaceHostMaps.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?, ?, ?)");

                    SpaceHostMap spaceHostMap = spaceHostMaps.get(i);
                    params.add(spaceHostMap.getId());
                    params.add(spaceHostMap.getSpaceId());
                    params.add(spaceHostMap.getHostId());
                    params.add(Timestamp.valueOf(spaceHostMap.getCreatedAt()));
                    params.add(Timestamp.valueOf(spaceHostMap.getUpdatedAt()));
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                spaceHostMapIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("SpaceHostMaps Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("SpaceHostMaps Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }

    private RepeatStatus processHostKakaos() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(hostKakaosFilePath))) {
            reader.readNext(); // Skip header

            for (long i = 0; i < hostKakaosProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<HostKakao> hostKakaos = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = hostKakaoIdCounter;

            while (count < 100 && (record = reader.readNext()) != null) {
                count++;
                hostKakaosProcessed++;

                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[0]), // host_id
                    record[1] // user_id
                );
                hostKakao.setId(idCounter++);
                hostKakaos.add(hostKakao);
            }

            if (!hostKakaos.isEmpty()) {
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO host_kakao (id, host_id, user_id) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < hostKakaos.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?)");

                    HostKakao hostKakao = hostKakaos.get(i);
                    params.add(hostKakao.getId());
                    params.add(hostKakao.getHostId());
                    params.add(hostKakao.getUserId());
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                hostKakaoIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("HostKakaos Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("HostKakaos Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }

    private RepeatStatus processGuests() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(guestsFilePath))) {
            reader.readNext(); // Skip header

            for (long i = 0; i < guestsProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<Guest> guests = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = guestIdCounter;

            while (count < 1000 && (record = reader.readNext()) != null) {
                count++;
                guestsProcessed++;

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
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < guests.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?, ?, ?)");

                    Guest guest = guests.get(i);
                    params.add(guest.getId());
                    params.add(guest.getSpaceId());
                    params.add(guest.getName());
                    params.add(Timestamp.valueOf(guest.getCreatedAt()));
                    params.add(Timestamp.valueOf(guest.getUpdatedAt()));
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                guestIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("Guests Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("Guests Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }

    private RepeatStatus processSpaceContents() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(spaceContentsFilePath))) {
            reader.readNext(); // Skip header

            for (long i = 0; i < spaceContentsProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<SpaceContent> spaceContents = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = spaceContentIdCounter;

            while (count < 20000 && (record = reader.readNext()) != null) {
                count++;
                spaceContentsProcessed++;

                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[0]), // content_type
                    Long.parseLong(record[1]), // space_id
                    Long.parseLong(record[2]) // guest_id
                );
                spaceContent.setId(idCounter++);
                spaceContents.add(spaceContent);
            }

            if (!spaceContents.isEmpty()) {
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < spaceContents.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?, ?)");

                    SpaceContent spaceContent = spaceContents.get(i);
                    params.add(spaceContent.getId());
                    params.add(spaceContent.getContentType().name());
                    params.add(spaceContent.getSpaceId());
                    params.add(spaceContent.getGuestId());
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                spaceContentIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("SpaceContents Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("SpaceContents Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }

    private RepeatStatus processPhotos() throws Exception {
        long startTime = System.currentTimeMillis();

        try (CSVReader reader = new CSVReader(new FileReader(photosFilePath))) {
            reader.readNext(); // Skip header

            for (long i = 0; i < photosProcessed; i++) {
                if (reader.readNext() == null) break;
            }

            List<Photo> photos = new ArrayList<>();
            String[] record;
            int count = 0;
            long idCounter = photoIdCounter;

            while (count < 20000 && (record = reader.readNext()) != null) {
                count++;
                photosProcessed++;

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
                // Use proper bulk insert with single SQL statement
                StringBuilder sql = new StringBuilder("INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES ");
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < photos.size(); i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("(?, ?, ?, ?, ?, ?)");

                    Photo photo = photos.get(i);
                    params.add(photo.getId());
                    params.add(photo.getOriginalName());
                    params.add(photo.getPath());
                    params.add(photo.getCapturedAt() != null ? Timestamp.valueOf(photo.getCapturedAt()) : null);
                    params.add(photo.getCapacity());
                    params.add(Timestamp.valueOf(photo.getCreatedAt()));
                }

                jdbcTemplate.update(sql.toString(), params.toArray());
                photoIdCounter = idCounter;
                long executionTime = System.currentTimeMillis() - startTime;
                System.out.printf("Photos Step: %d records (%dms)%n", count, executionTime);
            } else {
                System.out.println("Photos Step: No more data");
            }
            return RepeatStatus.FINISHED;
        }
    }
}
