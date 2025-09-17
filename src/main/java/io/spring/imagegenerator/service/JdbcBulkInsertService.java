package io.spring.imagegenerator.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.BiConsumer;

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
import jakarta.transaction.Transactional;

@Service
@Transactional
public class JdbcBulkInsertService {

    private static final int BATCH_SIZE = 1000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void bulkInsertSpaces(List<Space> spaces) {
        bulkInsertSpaces(spaces, null);
    }

    public void bulkInsertSpaces(List<Space> spaces, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        for (int i = 0; i < spaces.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, spaces.size());
            List<Space> batch = spaces.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    Space space = batch.get(idx);
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
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, spaces.size());
            }
        }
    }

    public void bulkInsertHosts(List<Host> hosts) {
        bulkInsertHosts(hosts, null);
    }

    public void bulkInsertHosts(List<Host> hosts, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        for (int i = 0; i < hosts.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, hosts.size());
            List<Host> batch = hosts.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    Host host = batch.get(idx);
                    ps.setLong(1, host.getId());
                    ps.setString(2, host.getName());
                    ps.setString(3, host.getPictureUrl());
                    ps.setBoolean(4, host.getAgreedTerms());
                    ps.setTimestamp(5, Timestamp.valueOf(host.getCreatedAt()));
                    ps.setTimestamp(6, Timestamp.valueOf(host.getUpdatedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, hosts.size());
            }
        }
    }

    public void bulkInsertSpaceHostMaps(List<SpaceHostMap> spaceHostMaps) {
        bulkInsertSpaceHostMaps(spaceHostMaps, null);
    }

    public void bulkInsertSpaceHostMaps(List<SpaceHostMap> spaceHostMaps, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        
        for (int i = 0; i < spaceHostMaps.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, spaceHostMaps.size());
            List<SpaceHostMap> batch = spaceHostMaps.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    SpaceHostMap spaceHostMap = batch.get(idx);
                    ps.setLong(1, spaceHostMap.getId());
                    ps.setLong(2, spaceHostMap.getSpaceId());
                    ps.setLong(3, spaceHostMap.getHostId());
                    ps.setTimestamp(4, Timestamp.valueOf(spaceHostMap.getCreatedAt()));
                    ps.setTimestamp(5, Timestamp.valueOf(spaceHostMap.getUpdatedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, spaceHostMaps.size());
            }
        }
    }

    public void bulkInsertHostKakaos(List<HostKakao> hostKakaos) {
        bulkInsertHostKakaos(hostKakaos, null);
    }

    public void bulkInsertHostKakaos(List<HostKakao> hostKakaos, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)";
        
        for (int i = 0; i < hostKakaos.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, hostKakaos.size());
            List<HostKakao> batch = hostKakaos.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    HostKakao hostKakao = batch.get(idx);
                    ps.setLong(1, hostKakao.getId());
                    ps.setLong(2, hostKakao.getHostId());
                    ps.setString(3, hostKakao.getUserId());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, hostKakaos.size());
            }
        }
    }

    public void bulkInsertGuests(List<Guest> guests) {
        bulkInsertGuests(guests, null);
    }

    public void bulkInsertGuests(List<Guest> guests, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        
        for (int i = 0; i < guests.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, guests.size());
            List<Guest> batch = guests.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    Guest guest = batch.get(idx);
                    ps.setLong(1, guest.getId());
                    ps.setLong(2, guest.getSpaceId());
                    ps.setString(3, guest.getName());
                    ps.setTimestamp(4, Timestamp.valueOf(guest.getCreatedAt()));
                    ps.setTimestamp(5, Timestamp.valueOf(guest.getUpdatedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, guests.size());
            }
        }
    }

    public void bulkInsertSpaceContents(List<SpaceContent> spaceContents) {
        bulkInsertSpaceContents(spaceContents, null);
    }

    public void bulkInsertSpaceContents(List<SpaceContent> spaceContents, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)";
        
        for (int i = 0; i < spaceContents.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, spaceContents.size());
            List<SpaceContent> batch = spaceContents.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    SpaceContent spaceContent = batch.get(idx);
                    ps.setLong(1, spaceContent.getId());
                    ps.setString(2, spaceContent.getContentType().name());
                    ps.setLong(3, spaceContent.getSpaceId());
                    ps.setLong(4, spaceContent.getGuestId());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, spaceContents.size());
            }
        }
    }

    public void bulkInsertPhotos(List<Photo> photos) {
        bulkInsertPhotos(photos, null);
    }

    public void bulkInsertPhotos(List<Photo> photos, BiConsumer<Integer, Integer> progressCallback) {
        String sql = "INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        for (int i = 0; i < photos.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, photos.size());
            List<Photo> batch = photos.subList(i, endIndex);
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    Photo photo = batch.get(idx);
                    ps.setLong(1, photo.getId());
                    ps.setString(2, photo.getOriginalName());
                    ps.setString(3, photo.getPath());
                    ps.setTimestamp(4, Timestamp.valueOf(photo.getCapturedAt()));
                    ps.setLong(5, photo.getCapacity());
                    ps.setTimestamp(6, Timestamp.valueOf(photo.getCreatedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            if (progressCallback != null) {
                progressCallback.accept(endIndex, photos.size());
            }
        }
    }
}
