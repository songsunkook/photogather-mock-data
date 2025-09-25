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
public class JdbcBulkInsertService {

    private static final int BATCH_SIZE = 1000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void bulkInsertSpaces(List<Space> spaces) {
        bulkInsertSpaces(spaces, null);
    }

    public void bulkInsertSpaces(List<Space> spaces, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < spaces.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, spaces.size());
            List<Space> batch = spaces.subList(i, endIndex);
            
            
            StringBuilder sql = new StringBuilder("INSERT INTO space (code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?, ?, ?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 8];
            int paramIndex = 0;
            
            for (Space space : batch) {
                params[paramIndex++] = space.getCode();
                params[paramIndex++] = space.getName();
                params[paramIndex++] = space.getValidHours();
                params[paramIndex++] = Timestamp.valueOf(space.getOpenedAt());
                params[paramIndex++] = space.getMaxCapacity();
                params[paramIndex++] = space.getType().name();
                params[paramIndex++] = Timestamp.valueOf(space.getCreatedAt());
                params[paramIndex++] = Timestamp.valueOf(space.getUpdatedAt());
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, spaces.size());
            }
        }
    }

    public void bulkInsertHosts(List<Host> hosts) {
        bulkInsertHosts(hosts, null);
    }

    public void bulkInsertHosts(List<Host> hosts, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < hosts.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, hosts.size());
            List<Host> batch = hosts.subList(i, endIndex);
            
            
            StringBuilder sql = new StringBuilder("INSERT INTO host (name, picture_url, agreed_terms, created_at, updated_at) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 5];
            int paramIndex = 0;
            
            for (Host host : batch) {
                params[paramIndex++] = host.getName();
                params[paramIndex++] = host.getPictureUrl();
                params[paramIndex++] = host.getAgreedTerms();
                params[paramIndex++] = Timestamp.valueOf(host.getCreatedAt());
                params[paramIndex++] = Timestamp.valueOf(host.getUpdatedAt());
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, hosts.size());
            }
        }
    }

    public void bulkInsertSpaceHostMaps(List<SpaceHostMap> spaceHostMaps) {
        bulkInsertSpaceHostMaps(spaceHostMaps, null);
    }

    public void bulkInsertSpaceHostMaps(List<SpaceHostMap> spaceHostMaps, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < spaceHostMaps.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, spaceHostMaps.size());
            List<SpaceHostMap> batch = spaceHostMaps.subList(i, endIndex);
            
            
            StringBuilder sql = new StringBuilder("INSERT INTO space_host_map (space_id, host_id, created_at, updated_at) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 4];
            int paramIndex = 0;
            
            for (SpaceHostMap spaceHostMap : batch) {
                params[paramIndex++] = spaceHostMap.getSpaceId();
                params[paramIndex++] = spaceHostMap.getHostId();
                params[paramIndex++] = Timestamp.valueOf(spaceHostMap.getCreatedAt());
                params[paramIndex++] = Timestamp.valueOf(spaceHostMap.getUpdatedAt());
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, spaceHostMaps.size());
            }
        }
    }

    public void bulkInsertHostKakaos(List<HostKakao> hostKakaos) {
        bulkInsertHostKakaos(hostKakaos, null);
    }

    public void bulkInsertHostKakaos(List<HostKakao> hostKakaos, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < hostKakaos.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, hostKakaos.size());
            List<HostKakao> batch = hostKakaos.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO host_kakao (host_id, user_id) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 2];
            int paramIndex = 0;
            
            for (HostKakao hostKakao : batch) {
                params[paramIndex++] = hostKakao.getHostId();
                params[paramIndex++] = hostKakao.getUserId();
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, hostKakaos.size());
            }
        }
    }

    public void bulkInsertGuests(List<Guest> guests) {
        bulkInsertGuests(guests, null);
    }

    public void bulkInsertGuests(List<Guest> guests, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < guests.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, guests.size());
            List<Guest> batch = guests.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO guest (space_id, name, created_at, updated_at) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 4];
            int paramIndex = 0;
            
            for (Guest guest : batch) {
                params[paramIndex++] = guest.getSpaceId();
                params[paramIndex++] = guest.getName();
                params[paramIndex++] = Timestamp.valueOf(guest.getCreatedAt());
                params[paramIndex++] = Timestamp.valueOf(guest.getUpdatedAt());
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, guests.size());
            }
        }
    }

    public void bulkInsertSpaceContents(List<SpaceContent> spaceContents) {
        bulkInsertSpaceContents(spaceContents, null);
    }

    public void bulkInsertSpaceContents(List<SpaceContent> spaceContents, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < spaceContents.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, spaceContents.size());
            List<SpaceContent> batch = spaceContents.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO space_content (content_type, space_id, guest_id) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 3];
            int paramIndex = 0;
            
            for (SpaceContent spaceContent : batch) {
                params[paramIndex++] = spaceContent.getContentType().name();
                params[paramIndex++] = spaceContent.getSpaceId();
                params[paramIndex++] = spaceContent.getGuestId();
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, spaceContents.size());
            }
        }
    }

    public void bulkInsertPhotos(List<Photo> photos) {
        bulkInsertPhotos(photos, null);
    }

    public void bulkInsertPhotos(List<Photo> photos, BiConsumer<Integer, Integer> progressCallback) {
        for (int i = 0; i < photos.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, photos.size());
            List<Photo> batch = photos.subList(i, endIndex);
            
            StringBuilder sql = new StringBuilder("INSERT INTO photo (original_name, path, captured_at, capacity, created_at) VALUES ");
            
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) sql.append(", ");
                sql.append("(?, ?, ?, ?, ?)");
            }
            
            Object[] params = new Object[batch.size() * 5];
            int paramIndex = 0;
            
            for (Photo photo : batch) {
                params[paramIndex++] = photo.getOriginalName();
                params[paramIndex++] = photo.getPath();
                params[paramIndex++] = photo.getCapturedAt() != null ? Timestamp.valueOf(photo.getCapturedAt()) : null;
                params[paramIndex++] = photo.getCapacity();
                params[paramIndex++] = Timestamp.valueOf(photo.getCreatedAt());
            }
            
            jdbcTemplate.update(sql.toString(), params);

            if (progressCallback != null) {
                progressCallback.accept(endIndex, photos.size());
            }
        }
    }
}