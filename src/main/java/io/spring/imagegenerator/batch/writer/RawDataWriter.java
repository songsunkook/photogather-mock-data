package io.spring.imagegenerator.batch.writer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RawDataWriter implements ItemWriter<Map<String, Object>> {
    
    @Autowired
    private DataSource dataSource;
    
    private long processedSpaces = 0;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

            // PreparedStatements 준비
            String spaceSql = "INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String hostSql = "INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
            String spaceHostMapSql = "INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            String hostKakaoSql = "INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)";
            String guestSql = "INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            String spaceContentSql = "INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)";
            String photoSql = "INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement spacePs = connection.prepareStatement(spaceSql);
                 PreparedStatement hostPs = connection.prepareStatement(hostSql);
                 PreparedStatement spaceHostMapPs = connection.prepareStatement(spaceHostMapSql);
                 PreparedStatement hostKakaoPs = connection.prepareStatement(hostKakaoSql);
                 PreparedStatement guestPs = connection.prepareStatement(guestSql);
                 PreparedStatement spaceContentPs = connection.prepareStatement(spaceContentSql);
                 PreparedStatement photoPs = connection.prepareStatement(photoSql)) {
                
                LocalDateTime now = LocalDateTime.now();
                
                for (Map<String, Object> rawData : chunk.getItems()) {
                    long spaceId = (Long) rawData.get("spaceId");
                    
                    // Space 삽입 (null 체크와 기본값 적용)
                    spacePs.setLong(1, spaceId);
                    spacePs.setString(2, (String) rawData.getOrDefault("code", "DEFAULT_CODE_" + spaceId));
                    spacePs.setString(3, (String) rawData.getOrDefault("name", "Default Space " + spaceId));
                    spacePs.setInt(4, (Integer) rawData.getOrDefault("validHours", 24));
                    String openedAtStr = (String) rawData.get("openedAt");
                    spacePs.setTimestamp(5, openedAtStr != null ? 
                        Timestamp.valueOf(LocalDateTime.parse(openedAtStr, dateTimeFormatter)) : 
                        Timestamp.valueOf(now));
                    spacePs.setLong(6, (Long) rawData.getOrDefault("maxCapacity", 100L));
                    String typeStr = (String) rawData.getOrDefault("type", "PRIVATE");
                    spacePs.setString(7, typeStr);
                    String createdAtStr = (String) rawData.get("createdAt");
                    spacePs.setTimestamp(8, createdAtStr != null ? 
                        Timestamp.valueOf(LocalDateTime.parse(createdAtStr, dateTimeFormatter)) : 
                        Timestamp.valueOf(now));
                    String updatedAtStr = (String) rawData.get("updatedAt");
                    spacePs.setTimestamp(9, updatedAtStr != null ? 
                        Timestamp.valueOf(LocalDateTime.parse(updatedAtStr, dateTimeFormatter)) : 
                        Timestamp.valueOf(now));
                    spacePs.addBatch();
                    
                    // Host 삽입
                    @SuppressWarnings("unchecked")
                    Map<String, Object> hostData = (Map<String, Object>) rawData.get("host");
                    if (hostData != null) {
                        long hostId = (Long) hostData.get("hostId");
                        
                        hostPs.setLong(1, hostId);
                        hostPs.setString(2, (String) hostData.getOrDefault("name", "Default Host " + hostId));
                        hostPs.setString(3, (String) hostData.getOrDefault("pictureUrl", ""));
                        hostPs.setBoolean(4, (Boolean) hostData.getOrDefault("agreedTerms", true));
                        String hostCreatedAtStr = (String) hostData.get("createdAt");
                        hostPs.setTimestamp(5, hostCreatedAtStr != null ? 
                            Timestamp.valueOf(LocalDateTime.parse(hostCreatedAtStr, dateTimeFormatter)) : 
                            Timestamp.valueOf(now));
                        String hostUpdatedAtStr = (String) hostData.get("updatedAt");
                        hostPs.setTimestamp(6, hostUpdatedAtStr != null ? 
                            Timestamp.valueOf(LocalDateTime.parse(hostUpdatedAtStr, dateTimeFormatter)) : 
                            Timestamp.valueOf(now));
                        hostPs.addBatch();
                        
                        // SpaceHostMap
                        spaceHostMapPs.setLong(1, spaceId * 1000); // 임시 ID
                        spaceHostMapPs.setLong(2, spaceId);
                        spaceHostMapPs.setLong(3, hostId);
                        spaceHostMapPs.setTimestamp(4, Timestamp.valueOf(now));
                        spaceHostMapPs.setTimestamp(5, Timestamp.valueOf(now));
                        spaceHostMapPs.addBatch();
                        
                        // HostKakao
                        @SuppressWarnings("unchecked")
                        Map<String, Object> kakaoData = (Map<String, Object>) hostData.get("kakao");
                        if (kakaoData != null) {
                            String userId = (String) kakaoData.get("userId");
                            if (userId != null && !userId.isEmpty()) {
                                hostKakaoPs.setLong(1, hostId * 1000); // 임시 ID
                                hostKakaoPs.setLong(2, hostId);
                                hostKakaoPs.setString(3, userId);
                                hostKakaoPs.addBatch();
                            }
                        }
                    }
                    
                    // Guest 및 Photo 대량 생성 (null 체크 강화)
                    Integer guestCount = (Integer) rawData.get("guestCount");
                    Integer photoCount = (Integer) rawData.get("photoCount");
                    if (guestCount != null && guestCount > 0) {
                        Long startGuestId = (Long) rawData.get("startGuestId");
                        Long startContentId = (Long) rawData.get("startContentId");
                        
                        if (startGuestId == null || startContentId == null) {
                            System.err.println("Warning: startGuestId or startContentId is null, skipping guest/photo creation");
                            continue;
                        }
                        
                        // Guest 생성 (10개씩)
                        for (int i = 0; i < guestCount; i++) {
                            long guestId = startGuestId + i;
                            
                            guestPs.setLong(1, guestId);
                            guestPs.setLong(2, spaceId);
                            guestPs.setString(3, "Guest" + i);
                            guestPs.setTimestamp(4, Timestamp.valueOf(now));
                            guestPs.setTimestamp(5, Timestamp.valueOf(now));
                            guestPs.addBatch();
                        }
                        
                        // Photo 생성 (200개씩)
                        if (photoCount != null && photoCount > 0) {
                            long photoPerGuest = photoCount / guestCount;
                            for (int i = 0; i < guestCount; i++) {
                                long guestId = startGuestId + i;
                                for (int j = 0; j < photoPerGuest; j++) {
                                    long contentId = startContentId + (i * photoPerGuest) + j;
                                    
                                    // SpaceContent
                                    spaceContentPs.setLong(1, contentId);
                                    spaceContentPs.setString(2, "PHOTO");
                                    spaceContentPs.setLong(3, spaceId);
                                    spaceContentPs.setLong(4, guestId);
                                    spaceContentPs.addBatch();
                                    
                                    // Photo
                                    photoPs.setLong(1, contentId);
                                    photoPs.setString(2, "photo_" + contentId + ".jpg");
                                    photoPs.setString(3, "/photos/photo_" + contentId + ".jpg");
                                    photoPs.setTimestamp(4, Timestamp.valueOf(now));
                                    photoPs.setLong(5, 1024);
                                    photoPs.setTimestamp(6, Timestamp.valueOf(now));
                                    photoPs.addBatch();
                                }
                            }
                        }
                        
                        totalGuests += guestCount;
                        totalPhotos += photoCount != null ? photoCount : 0;
                    }
                }
                
                // 배치 실행 (각 단계별 시간 측정)
                long step1Start = System.currentTimeMillis();
                spacePs.executeBatch();
                long step1End = System.currentTimeMillis();
                
                long step2Start = 0, step2End = 0, step3Start = 0, step3End = 0, step4Start = 0, step4End = 0, step5Start = 0, step5End = 0, step6Start = 0, step6End = 0;
                
                if (totalGuests > 0) {
                    step2Start = System.currentTimeMillis();
                    hostPs.executeBatch();
                    step2End = System.currentTimeMillis();
                    
                    step3Start = System.currentTimeMillis();
                    spaceHostMapPs.executeBatch();
                    step3End = System.currentTimeMillis();
                    
                    // hostKakao는 데이터가 있을 때만 실행
                    try {
                        step4Start = System.currentTimeMillis();
                        hostKakaoPs.executeBatch();
                        step4End = System.currentTimeMillis();
                    } catch (SQLException e) {
                        System.err.println("Warning: Failed to insert host_kakao data: " + e.getMessage());
                        step4End = System.currentTimeMillis();
                    }
                    
                    step5Start = System.currentTimeMillis();
                    guestPs.executeBatch();
                    step5End = System.currentTimeMillis();
                    
                    step6Start = System.currentTimeMillis();
                    spaceContentPs.executeBatch();
                    photoPs.executeBatch();
                    step6End = System.currentTimeMillis();
                }
                
                // 각 단계별 시간 출력
                System.out.printf("    [Timing] Space: %dms, Host: %dms, SpaceHostMap: %dms, HostKakao: %dms, Guest: %dms, Content+Photo: %dms\n", 
                    (step1End - step1Start), 
                    step2End > 0 ? (step2End - step2Start) : 0,
                    step3End > 0 ? (step3End - step3Start) : 0,
                    step4End > 0 ? (step4End - step4Start) : 0,
                    step5End > 0 ? (step5End - step5Start) : 0,
                    step6End > 0 ? (step6End - step6Start) : 0);
                
            }
            
        } catch (SQLException e) {
            System.err.println("[Raw Data Writer] SQL Error: " + e.getMessage());
            throw new RuntimeException("Failed to write raw data", e);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        
        long writeEndTime = System.currentTimeMillis();
        processedSpaces += chunk.size();
        
        System.out.printf("  ✓ [Raw Data Writer] Chunk %d items: %d spaces, %d guests, %d photos - %d ms (%.1f sec)\n", 
            chunk.size(), chunk.size(), totalGuests, totalPhotos, 
            (writeEndTime - writeStartTime), (writeEndTime - writeStartTime) / 1000.0);
        System.out.printf("  📊 [Raw Data Writer] Total processed: %d spaces\n", processedSpaces);
    }
}
