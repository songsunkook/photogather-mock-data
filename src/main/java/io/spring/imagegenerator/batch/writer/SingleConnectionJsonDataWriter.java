package io.spring.imagegenerator.batch.writer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.spring.imagegenerator.batch.processor.JsonProcessedData;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

@Component
public class SingleConnectionJsonDataWriter implements ItemWriter<JsonProcessedData> {
    
    @Autowired
    private DataSource dataSource;
    
    private long processedSpaces = 0;
    private static final long REPORT_INTERVAL = 100;
    private boolean optimizationApplied = false;

    @Override
    public void write(Chunk<? extends JsonProcessedData> chunk) throws Exception {
        List<Space> spaces = new ArrayList<>();
        List<Host> hosts = new ArrayList<>();
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        List<HostKakao> hostKakaos = new ArrayList<>();
        List<Guest> guests = new ArrayList<>();
        List<SpaceContent> spaceContents = new ArrayList<>();
        List<Photo> photos = new ArrayList<>();
        
        // 청크에서 데이터 수집
        for (JsonProcessedData processedData : chunk.getItems()) {
            if (processedData.getSpace() != null) {
                spaces.add(processedData.getSpace());
            }
            
            if (processedData.getHost() != null) {
                hosts.add(processedData.getHost());
            }
            
            if (processedData.getSpaceHostMap() != null) {
                spaceHostMaps.add(processedData.getSpaceHostMap());
            }
            
            if (processedData.getHostKakao() != null) {
                hostKakaos.add(processedData.getHostKakao());
            }
            
            if (processedData.getGuests() != null) {
                guests.addAll(processedData.getGuests());
            }
            
            if (processedData.getSpaceContents() != null) {
                spaceContents.addAll(processedData.getSpaceContents());
            }
            
            if (processedData.getPhotos() != null) {
                photos.addAll(processedData.getPhotos());
            }
        }
        
        // 단일 Connection으로 모든 테이블 처리
        long writeStartTime = System.currentTimeMillis();
        Connection connection = null;
        
        try {
            // Spring이 관리하는 트랜잭션 Connection 사용
            connection = DataSourceUtils.getConnection(dataSource);
            
            System.out.printf("[DEBUG] Connection: %s, Transaction: %s\n", 
                connection.toString(), TransactionSynchronizationManager.getCurrentTransactionName());
            
            // MySQL 최적화 상태 확인 및 적용
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                // 현재 설정 확인
                try (var rs = connection.createStatement().executeQuery("SELECT @@unique_checks, @@foreign_key_checks")) {
                    if (rs.next()) {
                        int uniqueChecks = rs.getInt(1);
                        int foreignKeyChecks = rs.getInt(2);
                        System.out.printf("[DEBUG] Before optimization - unique_checks: %d, foreign_key_checks: %d\n", 
                            uniqueChecks, foreignKeyChecks);
                    }
                }
                
                System.out.println("[Single Connection Writer] Applying MySQL optimizations");
                connection.createStatement().execute("SET SESSION unique_checks = 0");
                connection.createStatement().execute("SET SESSION foreign_key_checks = 0");
                
                // 적용 후 설정 확인
                try (var rs = connection.createStatement().executeQuery("SELECT @@unique_checks, @@foreign_key_checks")) {
                    if (rs.next()) {
                        int uniqueChecks = rs.getInt(1);
                        int foreignKeyChecks = rs.getInt(2);
                        System.out.printf("[DEBUG] After optimization - unique_checks: %d, foreign_key_checks: %d\n", 
                            uniqueChecks, foreignKeyChecks);
                    }
                }
                System.out.println("[Single Connection Writer] MySQL optimizations applied");
            }
            
            // 순서대로 batch insert 실행 (단일 Connection 사용)
            if (!spaces.isEmpty()) {
                insertSpaces(connection, spaces);
            }
            
            if (!hosts.isEmpty()) {
                insertHosts(connection, hosts);
            }
            
            if (!spaceHostMaps.isEmpty()) {
                insertSpaceHostMaps(connection, spaceHostMaps);
            }
            
            if (!hostKakaos.isEmpty()) {
                insertHostKakaos(connection, hostKakaos);
            }
            
            if (!guests.isEmpty()) {
                insertGuests(connection, guests);
            }
            
            if (!spaceContents.isEmpty()) {
                insertSpaceContents(connection, spaceContents);
            }
            
            if (!photos.isEmpty()) {
                insertPhotos(connection, photos);
            }
            
        } catch (SQLException e) {
            System.err.println("[Single Connection Writer] SQL Error: " + e.getMessage());
            throw new RuntimeException("Failed to write data with single connection", e);
        } finally {
            // Connection을 Spring에 반환
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        
        long writeEndTime = System.currentTimeMillis();
        processedSpaces += spaces.size();
        
        // 진행 상황 리포트
        if (processedSpaces % REPORT_INTERVAL == 0 || 
            (processedSpaces % REPORT_INTERVAL < spaces.size() && processedSpaces >= REPORT_INTERVAL)) {
            long reportCount = (processedSpaces / REPORT_INTERVAL) * REPORT_INTERVAL;
            if (reportCount > 0) {
                System.out.printf("  ✓ [Single Connection] Space #%d inserted (%d hosts, %d guests, %d photos) in %d ms\n", 
                    processedSpaces, hosts.size(), guests.size(), photos.size(), (writeEndTime - writeStartTime));
            }
        }
    }
    
    private void insertSpaces(Connection connection, List<Space> spaces) throws SQLException {
        String sql = "INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Space space : spaces) {
                ps.setLong(1, space.getId());
                ps.setString(2, space.getCode());
                ps.setString(3, space.getName());
                ps.setInt(4, space.getValidHours());
                ps.setTimestamp(5, Timestamp.valueOf(space.getOpenedAt()));
                ps.setLong(6, space.getMaxCapacity());
                ps.setString(7, space.getType().name());
                ps.setTimestamp(8, Timestamp.valueOf(space.getCreatedAt()));
                ps.setTimestamp(9, Timestamp.valueOf(space.getUpdatedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    private void insertHosts(Connection connection, List<Host> hosts) throws SQLException {
        String sql = "INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Host host : hosts) {
                ps.setLong(1, host.getId());
                ps.setString(2, host.getName());
                ps.setString(3, host.getPictureUrl());
                ps.setBoolean(4, host.getAgreedTerms());
                ps.setTimestamp(5, Timestamp.valueOf(host.getCreatedAt()));
                ps.setTimestamp(6, Timestamp.valueOf(host.getUpdatedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    private void insertSpaceHostMaps(Connection connection, List<SpaceHostMap> spaceHostMaps) throws SQLException {
        String sql = "INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SpaceHostMap spaceHostMap : spaceHostMaps) {
                ps.setLong(1, spaceHostMap.getId());
                ps.setLong(2, spaceHostMap.getSpaceId());
                ps.setLong(3, spaceHostMap.getHostId());
                ps.setTimestamp(4, Timestamp.valueOf(spaceHostMap.getCreatedAt()));
                ps.setTimestamp(5, Timestamp.valueOf(spaceHostMap.getUpdatedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    private void insertHostKakaos(Connection connection, List<HostKakao> hostKakaos) throws SQLException {
        String sql = "INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (HostKakao hostKakao : hostKakaos) {
                ps.setLong(1, hostKakao.getId());
                ps.setLong(2, hostKakao.getHostId());
                ps.setString(3, hostKakao.getUserId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    private void insertGuests(Connection connection, List<Guest> guests) throws SQLException {
        String sql = "INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Guest guest : guests) {
                ps.setLong(1, guest.getId());
                ps.setLong(2, guest.getSpaceId());
                ps.setString(3, guest.getName());
                ps.setTimestamp(4, Timestamp.valueOf(guest.getCreatedAt()));
                ps.setTimestamp(5, Timestamp.valueOf(guest.getUpdatedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    private void insertSpaceContents(Connection connection, List<SpaceContent> spaceContents) throws SQLException {
        String sql = "INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SpaceContent spaceContent : spaceContents) {
                ps.setLong(1, spaceContent.getId());
                ps.setString(2, spaceContent.getContentType().name());
                ps.setLong(3, spaceContent.getSpaceId());
                if (spaceContent.getGuestId() != null) {
                    ps.setLong(4, spaceContent.getGuestId());
                } else {
                    ps.setNull(4, java.sql.Types.BIGINT);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    private void insertPhotos(Connection connection, List<Photo> photos) throws SQLException {
        String sql = "INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Photo photo : photos) {
                ps.setLong(1, photo.getId());
                ps.setString(2, photo.getOriginalName());
                ps.setString(3, photo.getPath());
                ps.setTimestamp(4, photo.getCapturedAt() != null ? Timestamp.valueOf(photo.getCapturedAt()) : null);
                ps.setLong(5, photo.getCapacity());
                ps.setTimestamp(6, Timestamp.valueOf(photo.getCreatedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
