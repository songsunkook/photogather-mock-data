package io.spring.imagegenerator.batch.writer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.spring.imagegenerator.dto.JsonSpaceData;
import io.spring.imagegenerator.entity.Space;

@Component
public class DirectInsertJsonDataWriter implements ItemWriter<JsonSpaceData.SpaceData> {
    
    @Autowired
    private DataSource dataSource;
    
    private long processedSpaces = 0;
    private static final long REPORT_INTERVAL = 100;
    
    // ID 생성기
    private AtomicLong spaceIdGenerator = new AtomicLong(5101);
    private AtomicLong hostIdGenerator = new AtomicLong(5101);
    private AtomicLong guestIdGenerator = new AtomicLong(51001);
    private AtomicLong spaceContentIdGenerator = new AtomicLong(1020001);

    @Override
    public void write(Chunk<? extends JsonSpaceData.SpaceData> chunk) throws Exception {
        long writeStartTime = System.currentTimeMillis();
        Connection connection = null;
        
        // 통계 카운터
        int spaceCount = 0;
        int hostCount = 0;
        int guestCount = 0;
        int photoCount = 0;
        
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            
            System.out.printf("[Direct Insert] Processing %d spaces directly to DB\n", chunk.size());
            
            // MySQL 최적화 확인
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                System.out.println("[Direct Insert] Applying MySQL optimizations");
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
                
                // 직접 데이터 처리 (객체 생성 최소화)
                for (JsonSpaceData.SpaceData spaceData : chunk.getItems()) {
                    long spaceId = spaceIdGenerator.getAndIncrement();
                    
                    // 1. Space 직접 삽입
                    spacePs.setLong(1, spaceId);
                    spacePs.setString(2, spaceData.getCode());
                    spacePs.setString(3, spaceData.getName());
                    spacePs.setInt(4, spaceData.getValidHours());
                    spacePs.setTimestamp(5, Timestamp.valueOf(spaceData.getOpenedAt()));
                    spacePs.setLong(6, spaceData.getMaxCapacity());
                    spacePs.setString(7, Space.SpaceType.valueOf(spaceData.getType()).name());
                    spacePs.setTimestamp(8, Timestamp.valueOf(spaceData.getCreatedAt()));
                    spacePs.setTimestamp(9, Timestamp.valueOf(spaceData.getUpdatedAt()));
                    spacePs.addBatch();
                    spaceCount++;
                    
                    // 2. Host 직접 삽입
                    if (spaceData.getHost() != null) {
                        long hostId = hostIdGenerator.getAndIncrement();
                        
                        hostPs.setLong(1, hostId);
                        hostPs.setString(2, spaceData.getHost().getName());
                        hostPs.setString(3, spaceData.getHost().getPictureUrl());
                        hostPs.setBoolean(4, spaceData.getHost().getAgreedTerms());
                        hostPs.setTimestamp(5, Timestamp.valueOf(spaceData.getHost().getCreatedAt()));
                        hostPs.setTimestamp(6, Timestamp.valueOf(spaceData.getHost().getUpdatedAt()));
                        hostPs.addBatch();
                        hostCount++;
                        
                        // SpaceHostMap
                        spaceHostMapPs.setLong(1, spaceIdGenerator.getAndIncrement());
                        spaceHostMapPs.setLong(2, spaceId);
                        spaceHostMapPs.setLong(3, hostId);
                        spaceHostMapPs.setTimestamp(4, Timestamp.valueOf(now));
                        spaceHostMapPs.setTimestamp(5, Timestamp.valueOf(now));
                        spaceHostMapPs.addBatch();
                        
                        // HostKakao
                        if (spaceData.getHost().getKakao() != null) {
                            hostKakaoPs.setLong(1, spaceIdGenerator.getAndIncrement());
                            hostKakaoPs.setLong(2, hostId);
                            hostKakaoPs.setString(3, spaceData.getHost().getKakao().getUserId());
                            hostKakaoPs.addBatch();
                        }
                    }
                    
                    // 3. Guests 직접 삽입
                    if (spaceData.getGuests() != null) {
                        for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                            long guestId = guestIdGenerator.getAndIncrement();
                            
                            guestPs.setLong(1, guestId);
                            guestPs.setLong(2, spaceId);
                            guestPs.setString(3, guestData.getName());
                            guestPs.setTimestamp(4, Timestamp.valueOf(guestData.getCreatedAt()));
                            guestPs.setTimestamp(5, Timestamp.valueOf(guestData.getUpdatedAt()));
                            guestPs.addBatch();
                            guestCount++;
                            
                            // 4. Photos 직접 삽입
                            if (guestData.getPhotos() != null) {
                                for (JsonSpaceData.PhotoData photoData : guestData.getPhotos()) {
                                    long spaceContentId = spaceContentIdGenerator.getAndIncrement();
                                    
                                    // SpaceContent
                                    spaceContentPs.setLong(1, spaceContentId);
                                    spaceContentPs.setString(2, photoData.getContentType());
                                    spaceContentPs.setLong(3, spaceId);
                                    spaceContentPs.setLong(4, guestId);
                                    spaceContentPs.addBatch();
                                    
                                    // Photo
                                    photoPs.setLong(1, spaceContentId);
                                    photoPs.setString(2, photoData.getOriginalName());
                                    photoPs.setString(3, photoData.getPath());
                                    photoPs.setTimestamp(4, photoData.getCapturedAt() != null ? 
                                        Timestamp.valueOf(photoData.getCapturedAt()) : null);
                                    photoPs.setLong(5, photoData.getCapacity());
                                    photoPs.setTimestamp(6, Timestamp.valueOf(
                                        photoData.getCreatedAt() != null ? photoData.getCreatedAt() : now));
                                    photoPs.addBatch();
                                    photoCount++;
                                }
                            }
                        }
                    }
                }
                
                // 모든 배치 실행
                System.out.println("[Direct Insert] Executing batches...");
                spacePs.executeBatch();
                if (hostCount > 0) hostPs.executeBatch();
                if (hostCount > 0) spaceHostMapPs.executeBatch();
                hostKakaoPs.executeBatch(); // 빈 배치도 실행 가능
                if (guestCount > 0) guestPs.executeBatch();
                if (photoCount > 0) spaceContentPs.executeBatch();
                if (photoCount > 0) photoPs.executeBatch();
                
            }
            
        } catch (SQLException e) {
            System.err.println("[Direct Insert] SQL Error: " + e.getMessage());
            throw new RuntimeException("Failed to write data with direct insert", e);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        
        long writeEndTime = System.currentTimeMillis();
        processedSpaces += spaceCount;
        
        System.out.printf("  ✓ [Direct Insert] %d spaces, %d hosts, %d guests, %d photos inserted in %d ms\n", 
            spaceCount, hostCount, guestCount, photoCount, (writeEndTime - writeStartTime));
    }
}
