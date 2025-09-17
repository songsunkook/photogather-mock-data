package io.spring.imagegenerator.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

@Service
public class JdbcBulkInsertService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void bulkInsertSpaces(List<Space> spaces) {
        String sql = "INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
        });
    }

    public void bulkInsertHosts(List<Host> hosts) {
        String sql = "INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
        });
    }

    public void bulkInsertSpaceHostMaps(List<SpaceHostMap> spaceHostMaps) {
        String sql = "INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
        });
    }

    public void bulkInsertHostKakaos(List<HostKakao> hostKakaos) {
        String sql = "INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
        });
    }

    public void bulkInsertGuests(List<Guest> guests) {
        String sql = "INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
        });
    }

    public void bulkInsertSpaceContents(List<SpaceContent> spaceContents) {
        String sql = "INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SpaceContent spaceContent = spaceContents.get(i);
                ps.setLong(1, spaceContent.getId());
                ps.setString(2, spaceContent.getContentType().name());
                ps.setLong(3, spaceContent.getSpaceId());
                ps.setLong(4, spaceContent.getGuestId());
            }

            @Override
            public int getBatchSize() {
                return spaceContents.size();
            }
        });
    }

    public void bulkInsertPhotos(List<Photo> photos) {
        String sql = "INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Photo photo = photos.get(i);
                ps.setLong(1, photo.getId());
                ps.setString(2, photo.getOriginalName());
                ps.setString(3, photo.getPath());
                ps.setTimestamp(4, Timestamp.valueOf(photo.getCapturedAt()));
                ps.setLong(5, photo.getCapacity());
                ps.setTimestamp(6, Timestamp.valueOf(photo.getCreatedAt()));
            }

            @Override
            public int getBatchSize() {
                return photos.size();
            }
        });
    }
}
