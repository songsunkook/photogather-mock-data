package io.spring.imagegenerator.batch.writer;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class MultiRowBulkWriter implements ItemWriter<Map<String, Object>> {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private long processedSpaces = 0;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BULK_SIZE = 1000;

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
        long writeStartTime = System.currentTimeMillis();
        Connection connection = null;
        
        int totalPhotos = 0;
        int totalGuests = 0;
        
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            
            // MySQL 최적화 적용
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                connection.createStatement().execute("SET SESSION unique_checks = 0");
                connection.createStatement().execute("SET SESSION foreign_key_checks = 0");
            }
            
            // 데이터 분류
            List<Map<String, Object>> spaces = new ArrayList<>();
            List<Map<String, Object>> hosts = new ArrayList<>();
            List<Map<String, Object>> spaceHostMaps = new ArrayList<>();
            List<Map<String, Object>> hostKakaos = new ArrayList<>();
            List<Map<String, Object>> guests = new ArrayList<>();
            List<Map<String, Object>> spaceContents = new ArrayList<>();
            List<Map<String, Object>> photos = new ArrayList<>();
            
            LocalDateTime now = LocalDateTime.now();
            
            // 데이터 분류 및 생성
            long step1Start = System.currentTimeMillis();
            for (Map<String, Object> rawData : chunk.getItems()) {
                long spaceId = (Long) rawData.get("spaceId");
                
                // Space 데이터 준비
                Map<String, Object> spaceMap = Map.of(
                    "id", spaceId,
                    "code", rawData.getOrDefault("code", "DEFAULT_CODE_" + spaceId),
                    "name", rawData.getOrDefault("name", "Default Space " + spaceId),
                    "validHours", rawData.getOrDefault("validHours", 24),
                    "openedAt", getTimestamp(rawData, "openedAt", now),
                    "maxCapacity", rawData.getOrDefault("maxCapacity", 100L),
                    "type", rawData.getOrDefault("type", "PRIVATE"),
                    "createdAt", getTimestamp(rawData, "createdAt", now),
                    "updatedAt", getTimestamp(rawData, "updatedAt", now)
                );
                spaces.add(spaceMap);
                
                // Host 데이터 처리
                @SuppressWarnings("unchecked")
                Map<String, Object> hostData = (Map<String, Object>) rawData.get("host");
                if (hostData != null && hostData.size() > 1) {
                    long hostId = (Long) hostData.get("hostId");
                    
                    Map<String, Object> hostMap = Map.of(
                        "id", hostId,
                        "name", hostData.getOrDefault("name", "Default Host " + hostId),
                        "pictureUrl", hostData.getOrDefault("pictureUrl", ""),
                        "agreedTerms", hostData.getOrDefault("agreedTerms", true),
                        "createdAt", getTimestamp(hostData, "createdAt", now),
                        "updatedAt", getTimestamp(hostData, "updatedAt", now)
                    );
                    hosts.add(hostMap);
                    
                    // SpaceHostMap
                    spaceHostMaps.add(Map.of(
                        "id", spaceId * 1000,
                        "spaceId", spaceId,
                        "hostId", hostId,
                        "createdAt", now,
                        "updatedAt", now
                    ));
                    
                    // HostKakao
                    @SuppressWarnings("unchecked")
                    Map<String, Object> kakaoData = (Map<String, Object>) hostData.get("kakao");
                    if (kakaoData != null && !kakaoData.isEmpty()) {
                        String userId = (String) kakaoData.get("userId");
                        if (userId != null && !userId.isEmpty()) {
                            hostKakaos.add(Map.of(
                                "id", hostId * 1000,
                                "hostId", hostId,
                                "userId", userId
                            ));
                        }
                    }
                }
                
                // Guest 및 Photo 대량 생성
                Integer guestCount = (Integer) rawData.get("guestCount");
                Integer photoCount = (Integer) rawData.get("photoCount");
                if (guestCount != null && guestCount > 0) {
                    Long startGuestId = (Long) rawData.get("startGuestId");
                    Long startContentId = (Long) rawData.get("startContentId");
                    
                    if (startGuestId != null && startContentId != null) {
                        // Guest 생성
                        for (int i = 0; i < guestCount; i++) {
                            long guestId = startGuestId + i;
                            guests.add(Map.of(
                                "id", guestId,
                                "spaceId", spaceId,
                                "name", "Guest" + i,
                                "createdAt", now,
                                "updatedAt", now
                            ));
                        }
                        
                        // Photo 생성
                        if (photoCount != null && photoCount > 0) {
                            long photoPerGuest = photoCount / guestCount;
                            for (int i = 0; i < guestCount; i++) {
                                long guestId = startGuestId + i;
                                for (int j = 0; j < photoPerGuest; j++) {
                                    long contentId = startContentId + (i * photoPerGuest) + j;
                                    
                                    spaceContents.add(Map.of(
                                        "id", contentId,
                                        "contentType", "PHOTO",
                                        "spaceId", spaceId,
                                        "guestId", guestId
                                    ));
                                    
                                    photos.add(Map.of(
                                        "id", contentId,
                                        "originalName", "photo_" + contentId + ".jpg",
                                        "path", "/photos/photo_" + contentId + ".jpg",
                                        "capturedAt", now,
                                        "capacity", 1024L,
                                        "createdAt", now
                                    ));
                                }
                            }
                        }
                        
                        totalGuests += guestCount;
                        totalPhotos += photoCount != null ? photoCount : 0;
                    }
                }
            }
            long step1End = System.currentTimeMillis();
            
            // Multi-row bulk insert 실행
            long step2Start = System.currentTimeMillis();
            bulkInsertSpaces(spaces);
            long step2End = System.currentTimeMillis();
            
            long step3Start = System.currentTimeMillis();
            if (!hosts.isEmpty()) bulkInsertHosts(hosts);
            if (!spaceHostMaps.isEmpty()) bulkInsertSpaceHostMaps(spaceHostMaps);
            if (!hostKakaos.isEmpty()) bulkInsertHostKakaos(hostKakaos);
            long step3End = System.currentTimeMillis();
            
            long step4Start = System.currentTimeMillis();
            if (!guests.isEmpty()) bulkInsertGuests(guests);
            long step4End = System.currentTimeMillis();
            
            long step5Start = System.currentTimeMillis();
            if (!spaceContents.isEmpty()) bulkInsertSpaceContents(spaceContents);
            if (!photos.isEmpty()) bulkInsertPhotos(photos);
            long step5End = System.currentTimeMillis();
            
            // 각 단계별 시간 출력
            System.out.printf("    [MultiRow Timing] DataBuild: %dms, Space: %dms, Host/Map/Kakao: %dms, Guest: %dms, Content+Photo: %dms\n", 
                (step1End - step1Start),
                (step2End - step2Start), 
                (step3End - step3Start),
                (step4End - step4Start),
                (step5End - step5Start));
            
        } catch (Exception e) {
            System.err.println("[MultiRow Bulk Writer] Error: " + e.getMessage());
            throw new RuntimeException("Failed to write bulk data", e);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        
        long writeEndTime = System.currentTimeMillis();
        processedSpaces += chunk.size();
        
        System.out.printf("  ✓ [MultiRow Bulk Writer] Chunk %d items: %d spaces, %d guests, %d photos - %d ms (%.1f sec)\n", 
            chunk.size(), chunk.size(), totalGuests, totalPhotos, 
            (writeEndTime - writeStartTime), (writeEndTime - writeStartTime) / 1000.0);
        System.out.printf("  📊 [MultiRow Bulk Writer] Total processed: %d spaces\n", processedSpaces);
    }
    
    private Timestamp getTimestamp(Map<String, Object> data, String key, LocalDateTime defaultTime) {
        String timeStr = (String) data.get(key);
        if (timeStr != null) {
            try {
                return Timestamp.valueOf(LocalDateTime.parse(timeStr, dateTimeFormatter));
            } catch (Exception e) {
                return Timestamp.valueOf(defaultTime);
            }
        }
        return Timestamp.valueOf(defaultTime);
    }
    
    // Multi-row INSERT 메서드들
    private void bulkInsertSpaces(List<Map<String, Object>> spaces) {
        if (spaces.isEmpty()) return;
        
        for (int i = 0; i < spaces.size(); i += BULK_SIZE) {
            int endIndex = Math.min(i + BULK_SIZE, spaces.size());
            List<Map<String, Object>> batch = spaces.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES ");
            List<Object> params = new ArrayList<>();
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?)");
                
                Map<String, Object> space = batch.get(j);
                params.add(space.get("id"));
                params.add(space.get("code"));
                params.add(space.get("name"));
                params.add(space.get("validHours"));
                params.add(space.get("openedAt"));
                params.add(space.get("maxCapacity"));
                params.add(space.get("type"));
                params.add(space.get("createdAt"));
                params.add(space.get("updatedAt"));
            }
            
            jdbcTemplate.update(sql.toString(), params.toArray());
        }
    }
    
    private void bulkInsertHosts(List<Map<String, Object>> hosts) {
        if (hosts.isEmpty()) return;
        
        for (int i = 0; i < hosts.size(); i += BULK_SIZE) {
            int endIndex = Math.min(i + BULK_SIZE, hosts.size());
            List<Map<String, Object>> batch = hosts.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES ");
            List<Object> params = new ArrayList<>();
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?, ?)");
                
                Map<String, Object> host = batch.get(j);
                params.add(host.get("id"));
                params.add(host.get("name"));
                params.add(host.get("pictureUrl"));
                params.add(host.get("agreedTerms"));
                params.add(host.get("createdAt"));
                params.add(host.get("updatedAt"));
            }
            
            jdbcTemplate.update(sql.toString(), params.toArray());
        }
    }
    
    private void bulkInsertSpaceHostMaps(List<Map<String, Object>> spaceHostMaps) {
        if (spaceHostMaps.isEmpty()) return;
        
        StringBuilder sql = new StringBuilder("INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES ");
        List<Object> params = new ArrayList<>();
        
        for (int i = 0; i < spaceHostMaps.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?, ?)");
            
            Map<String, Object> map = spaceHostMaps.get(i);
            params.add(map.get("id"));
            params.add(map.get("spaceId"));
            params.add(map.get("hostId"));
            params.add(map.get("createdAt"));
            params.add(map.get("updatedAt"));
        }
        
        jdbcTemplate.update(sql.toString(), params.toArray());
    }
    
    private void bulkInsertHostKakaos(List<Map<String, Object>> hostKakaos) {
        if (hostKakaos.isEmpty()) return;
        
        StringBuilder sql = new StringBuilder("INSERT INTO host_kakao (id, host_id, user_id) VALUES ");
        List<Object> params = new ArrayList<>();
        
        for (int i = 0; i < hostKakaos.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?)");
            
            Map<String, Object> kakao = hostKakaos.get(i);
            params.add(kakao.get("id"));
            params.add(kakao.get("hostId"));
            params.add(kakao.get("userId"));
        }
        
        jdbcTemplate.update(sql.toString(), params.toArray());
    }
    
    private void bulkInsertGuests(List<Map<String, Object>> guests) {
        if (guests.isEmpty()) return;
        
        for (int i = 0; i < guests.size(); i += BULK_SIZE) {
            int endIndex = Math.min(i + BULK_SIZE, guests.size());
            List<Map<String, Object>> batch = guests.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES ");
            List<Object> params = new ArrayList<>();
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?)");
                
                Map<String, Object> guest = batch.get(j);
                params.add(guest.get("id"));
                params.add(guest.get("spaceId"));
                params.add(guest.get("name"));
                params.add(guest.get("createdAt"));
                params.add(guest.get("updatedAt"));
            }
            
            jdbcTemplate.update(sql.toString(), params.toArray());
        }
    }
    
    private void bulkInsertSpaceContents(List<Map<String, Object>> spaceContents) {
        if (spaceContents.isEmpty()) return;
        
        for (int i = 0; i < spaceContents.size(); i += BULK_SIZE) {
            int endIndex = Math.min(i + BULK_SIZE, spaceContents.size());
            List<Map<String, Object>> batch = spaceContents.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES ");
            List<Object> params = new ArrayList<>();
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?)");
                
                Map<String, Object> content = batch.get(j);
                params.add(content.get("id"));
                params.add(content.get("contentType"));
                params.add(content.get("spaceId"));
                params.add(content.get("guestId"));
            }
            
            jdbcTemplate.update(sql.toString(), params.toArray());
        }
    }
    
    private void bulkInsertPhotos(List<Map<String, Object>> photos) {
        if (photos.isEmpty()) return;
        
        for (int i = 0; i < photos.size(); i += BULK_SIZE) {
            int endIndex = Math.min(i + BULK_SIZE, photos.size());
            List<Map<String, Object>> batch = photos.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES ");
            List<Object> params = new ArrayList<>();
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?, ?)");
                
                Map<String, Object> photo = batch.get(j);
                params.add(photo.get("id"));
                params.add(photo.get("originalName"));
                params.add(photo.get("path"));
                params.add(photo.get("capturedAt"));
                params.add(photo.get("capacity"));
                params.add(photo.get("createdAt"));
            }
            
            jdbcTemplate.update(sql.toString(), params.toArray());
        }
    }
}