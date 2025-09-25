package io.spring.imagegenerator.batch.writer;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.spring.imagegenerator.batch.processor.JsonProcessedData;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

@Component
public class JsonDataWriter implements ItemWriter<JsonProcessedData> {
    
    @Autowired
    private DataSource dataSource;
    
    private JdbcBatchItemWriter<Space> spaceWriter;
    private JdbcBatchItemWriter<Host> hostWriter;
    private JdbcBatchItemWriter<SpaceHostMap> spaceHostMapWriter;
    private JdbcBatchItemWriter<HostKakao> hostKakaoWriter;
    private JdbcBatchItemWriter<Guest> guestWriter;
    private JdbcBatchItemWriter<SpaceContent> spaceContentWriter;
    private JdbcBatchItemWriter<Photo> photoWriter;
    
    private long processedSpaces = 0;
    private static final long REPORT_INTERVAL = 100;
    
    private void initializeWriters() {
        if (spaceWriter == null) {
            spaceWriter = new JdbcBatchItemWriterBuilder<Space>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .itemPreparedStatementSetter((space, ps) -> {
                        ps.setLong(1, space.getId());
                        ps.setString(2, space.getCode());
                        ps.setString(3, space.getName());
                        ps.setInt(4, space.getValidHours());
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(space.getOpenedAt()));
                        ps.setLong(6, space.getMaxCapacity());
                        ps.setString(7, space.getType().name());
                        ps.setTimestamp(8, java.sql.Timestamp.valueOf(space.getCreatedAt()));
                        ps.setTimestamp(9, java.sql.Timestamp.valueOf(space.getUpdatedAt()));
                    })
                    .build();
            
            hostWriter = new JdbcBatchItemWriterBuilder<Host>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)")
                    .itemPreparedStatementSetter((host, ps) -> {
                        ps.setLong(1, host.getId());
                        ps.setString(2, host.getName());
                        ps.setString(3, host.getPictureUrl());
                        ps.setBoolean(4, host.getAgreedTerms());
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(host.getCreatedAt()));
                        ps.setTimestamp(6, java.sql.Timestamp.valueOf(host.getUpdatedAt()));
                    })
                    .build();
            
            spaceHostMapWriter = new JdbcBatchItemWriterBuilder<SpaceHostMap>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")
                    .itemPreparedStatementSetter((spaceHostMap, ps) -> {
                        ps.setLong(1, spaceHostMap.getId());
                        ps.setLong(2, spaceHostMap.getSpaceId());
                        ps.setLong(3, spaceHostMap.getHostId());
                        ps.setTimestamp(4, java.sql.Timestamp.valueOf(spaceHostMap.getCreatedAt()));
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(spaceHostMap.getUpdatedAt()));
                    })
                    .build();
            
            hostKakaoWriter = new JdbcBatchItemWriterBuilder<HostKakao>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)")
                    .itemPreparedStatementSetter((hostKakao, ps) -> {
                        ps.setLong(1, hostKakao.getId());
                        ps.setLong(2, hostKakao.getHostId());
                        ps.setString(3, hostKakao.getUserId());
                    })
                    .build();
            
            guestWriter = new JdbcBatchItemWriterBuilder<Guest>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")
                    .itemPreparedStatementSetter((guest, ps) -> {
                        ps.setLong(1, guest.getId());
                        ps.setLong(2, guest.getSpaceId());
                        ps.setString(3, guest.getName());
                        ps.setTimestamp(4, java.sql.Timestamp.valueOf(guest.getCreatedAt()));
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(guest.getUpdatedAt()));
                    })
                    .build();
            
            spaceContentWriter = new JdbcBatchItemWriterBuilder<SpaceContent>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)")
                    .itemPreparedStatementSetter((spaceContent, ps) -> {
                        ps.setLong(1, spaceContent.getId());
                        ps.setString(2, spaceContent.getContentType().name());
                        ps.setLong(3, spaceContent.getSpaceId());
                        if (spaceContent.getGuestId() != null) {
                            ps.setLong(4, spaceContent.getGuestId());
                        } else {
                            ps.setNull(4, java.sql.Types.BIGINT);
                        }
                    })
                    .build();
            
            photoWriter = new JdbcBatchItemWriterBuilder<Photo>()
                    .dataSource(dataSource)
                    .sql("INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)")
                    .itemPreparedStatementSetter((photo, ps) -> {
                        ps.setLong(1, photo.getId());
                        ps.setString(2, photo.getOriginalName());
                        ps.setString(3, photo.getPath());
                        if (photo.getCapturedAt() != null) {
                            ps.setTimestamp(4, java.sql.Timestamp.valueOf(photo.getCapturedAt()));
                        } else {
                            ps.setNull(4, java.sql.Types.TIMESTAMP);
                        }
                        ps.setLong(5, photo.getCapacity());
                        ps.setTimestamp(6, java.sql.Timestamp.valueOf(photo.getCreatedAt()));
                    })
                    .build();
        }
    }
    
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
        
        // Initialize writers if not already done
        initializeWriters();
        
        // 순서대로 batch insert 실행
        long writeStartTime = System.currentTimeMillis();
        
        if (!spaces.isEmpty()) {
            spaceWriter.write(new Chunk<>(spaces));
        }
        
        if (!hosts.isEmpty()) {
            hostWriter.write(new Chunk<>(hosts));
        }
        
        if (!spaceHostMaps.isEmpty()) {
            spaceHostMapWriter.write(new Chunk<>(spaceHostMaps));
        }
        
        if (!hostKakaos.isEmpty()) {
            hostKakaoWriter.write(new Chunk<>(hostKakaos));
        }
        
        if (!guests.isEmpty()) {
            guestWriter.write(new Chunk<>(guests));
        }
        
        if (!spaceContents.isEmpty()) {
            spaceContentWriter.write(new Chunk<>(spaceContents));
        }
        
        if (!photos.isEmpty()) {
            photoWriter.write(new Chunk<>(photos));
        }
        
        long writeEndTime = System.currentTimeMillis();
        processedSpaces += spaces.size();
        
        // 진행 상황 리포트
        if (processedSpaces % REPORT_INTERVAL == 0 || 
            (processedSpaces % REPORT_INTERVAL < spaces.size() && processedSpaces >= REPORT_INTERVAL)) {
            long reportCount = (processedSpaces / REPORT_INTERVAL) * REPORT_INTERVAL;
            if (reportCount > 0) {
                System.out.printf("  ✓ [JSON Writer] Space #%d inserted (%d hosts, %d guests, %d photos) in %d ms\n", 
                    processedSpaces, hosts.size(), guests.size(), photos.size(), (writeEndTime - writeStartTime));
            }
        } else {
            // 매 청크마다 현재 진행 상황 표시
            if (spaces.size() > 0) {
                System.out.printf("  → [JSON Writer] Space #%d inserted (%d hosts, %d guests, %d photos)\n", 
                    processedSpaces, hosts.size(), guests.size(), photos.size());
            }
        }
    }
}
