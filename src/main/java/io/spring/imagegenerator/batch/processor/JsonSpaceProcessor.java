package io.spring.imagegenerator.batch.processor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import io.spring.imagegenerator.dto.JsonSpaceData;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

@Component
public class JsonSpaceProcessor implements ItemProcessor<JsonSpaceData.SpaceData, JsonProcessedData> {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final AtomicLong spaceIdGenerator = new AtomicLong(1);
    private final AtomicLong hostIdGenerator = new AtomicLong(1);
    private final AtomicLong spaceHostMapIdGenerator = new AtomicLong(1);
    private final AtomicLong hostKakaoIdGenerator = new AtomicLong(1);
    private final AtomicLong guestIdGenerator = new AtomicLong(1);
    private final AtomicLong spaceContentIdGenerator = new AtomicLong(1);
    
    private volatile boolean initialized = false;
    
    @PostConstruct
    public void initializeIdGenerators() {
        System.out.println("[JsonSpaceProcessor] Initializing ID generators from database...");
        
        try {
            if (jdbcTemplate == null) {
                System.err.println("[JsonSpaceProcessor] JdbcTemplate is null, cannot initialize from database");
                return;
            }
            
            // 각 테이블의 최대 ID + 1로 초기화
            Long maxSpaceId = getMaxId("space");
            Long maxHostId = getMaxId("host");
            Long maxSpaceHostMapId = getMaxId("space_host_map");
            Long maxHostKakaoId = getMaxId("host_kakao");
            Long maxGuestId = getMaxId("guest");
            Long maxSpaceContentId = getMaxId("space_content");
            
            long nextSpaceId = maxSpaceId + 1;
            long nextHostId = maxHostId + 1;
            long nextSpaceHostMapId = maxSpaceHostMapId + 1;
            long nextHostKakaoId = maxHostKakaoId + 1;
            long nextGuestId = maxGuestId + 1;
            long nextSpaceContentId = maxSpaceContentId + 1;
            
            spaceIdGenerator.set(nextSpaceId);
            hostIdGenerator.set(nextHostId);
            spaceHostMapIdGenerator.set(nextSpaceHostMapId);
            hostKakaoIdGenerator.set(nextHostKakaoId);
            guestIdGenerator.set(nextGuestId);
            spaceContentIdGenerator.set(nextSpaceContentId);
            
            System.out.printf("[JsonSpaceProcessor] ✓ ID generators initialized - Space: %d, Host: %d, Guest: %d, SpaceContent: %d\n",
                nextSpaceId, nextHostId, nextGuestId, nextSpaceContentId);
                
        } catch (Exception e) {
            System.err.printf("[JsonSpaceProcessor] ERROR: Failed to initialize ID generators: %s\n", e.getMessage());
            e.printStackTrace();
            // 비상상황에서라도 안전한 시작값 설정
            spaceIdGenerator.set(10000);
            hostIdGenerator.set(10000);
            spaceHostMapIdGenerator.set(10000);
            hostKakaoIdGenerator.set(10000);
            guestIdGenerator.set(50000);
            spaceContentIdGenerator.set(1000000);
            System.out.println("[JsonSpaceProcessor] Using safe fallback ID values");
        }
    }
    
    private Long getMaxId(String tableName) {
        try {
            String sql = "SELECT COALESCE(MAX(id), 0) FROM " + tableName;
            Long maxId = jdbcTemplate.queryForObject(sql, Long.class);
            System.out.printf("[JsonSpaceProcessor] Table %s max ID: %d\n", tableName, maxId);
            return maxId != null ? maxId : 0L;
        } catch (Exception e) {
            System.err.printf("[JsonSpaceProcessor] Warning: Could not get max ID from table %s: %s\n", tableName, e.getMessage());
            return 0L;
        }
    }
    
    @Override
    public JsonProcessedData process(JsonSpaceData.SpaceData spaceData) throws Exception {
        // 처음 실행 시 ID 생성기 초기화 다시 시도
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    System.out.println("[JsonSpaceProcessor] First execution - initializing ID generators from database...");
                    initializeIdGenerators();
                    initialized = true;
                }
            }
        }
        
        JsonProcessedData processedData = new JsonProcessedData();
        
        // Space 생성
        Space space = createSpace(spaceData);
        processedData.setSpace(space);
        
        // Host 관련 데이터 생성
        if (spaceData.getHost() != null) {
            Host host = createHost(spaceData.getHost());
            processedData.setHost(host);
            
            SpaceHostMap spaceHostMap = createSpaceHostMap(space.getId(), host.getId());
            processedData.setSpaceHostMap(spaceHostMap);
            
            if (spaceData.getHost().getKakao() != null) {
                HostKakao hostKakao = createHostKakao(host.getId(), spaceData.getHost().getKakao());
                processedData.setHostKakao(hostKakao);
            }
        }
        
        // Guests 및 Photos 생성
        List<Guest> guests = new ArrayList<>();
        List<SpaceContent> spaceContents = new ArrayList<>();
        List<Photo> photos = new ArrayList<>();
        
        if (spaceData.getGuests() != null) {
            for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                Guest guest = createGuest(space.getId(), guestData);
                guests.add(guest);
                
                if (guestData.getPhotos() != null) {
                    for (JsonSpaceData.PhotoData photoData : guestData.getPhotos()) {
                        Long spaceContentId = spaceContentIdGenerator.getAndIncrement();
                        
                        SpaceContent spaceContent = createSpaceContent(spaceContentId, space.getId(), guest.getId(), photoData);
                        spaceContents.add(spaceContent);
                        
                        Photo photo = createPhoto(spaceContentId, photoData);
                        photos.add(photo);
                    }
                }
            }
        }
        
        processedData.setGuests(guests);
        processedData.setSpaceContents(spaceContents);
        processedData.setPhotos(photos);
        
        return processedData;
    }
    
    private Space createSpace(JsonSpaceData.SpaceData spaceData) {
        Space space = new Space(
            spaceData.getCode(),
            spaceData.getName(),
            spaceData.getValidHours(),
            spaceData.getOpenedAt(),
            spaceData.getMaxCapacity(),
            Space.SpaceType.valueOf(spaceData.getType()),
            spaceData.getCreatedAt(),
            spaceData.getUpdatedAt()
        );
        space.setId(spaceIdGenerator.getAndIncrement());
        return space;
    }
    
    private Host createHost(JsonSpaceData.HostData hostData) {
        Host host = new Host(
            hostData.getName(),
            hostData.getPictureUrl(),
            hostData.getAgreedTerms(),
            hostData.getCreatedAt(),
            hostData.getUpdatedAt()
        );
        host.setId(hostIdGenerator.getAndIncrement());
        return host;
    }
    
    private SpaceHostMap createSpaceHostMap(Long spaceId, Long hostId) {
        SpaceHostMap spaceHostMap = new SpaceHostMap(
            spaceId,
            hostId,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        spaceHostMap.setId(spaceHostMapIdGenerator.getAndIncrement());
        return spaceHostMap;
    }
    
    private HostKakao createHostKakao(Long hostId, JsonSpaceData.KakaoData kakaoData) {
        HostKakao hostKakao = new HostKakao(
            hostId,
            kakaoData.getUserId()
        );
        hostKakao.setId(hostKakaoIdGenerator.getAndIncrement());
        return hostKakao;
    }
    
    private Guest createGuest(Long spaceId, JsonSpaceData.GuestData guestData) {
        Guest guest = new Guest(
            spaceId,
            guestData.getName(),
            guestData.getCreatedAt(),
            guestData.getUpdatedAt()
        );
        guest.setId(guestIdGenerator.getAndIncrement());
        return guest;
    }
    
    private SpaceContent createSpaceContent(Long id, Long spaceId, Long guestId, JsonSpaceData.PhotoData photoData) {
        SpaceContent spaceContent = new SpaceContent(
            SpaceContent.ContentType.valueOf(photoData.getContentType()),
            spaceId,
            guestId
        );
        spaceContent.setId(id);
        return spaceContent;
    }
    
    private Photo createPhoto(Long id, JsonSpaceData.PhotoData photoData) {
        Photo photo = new Photo(
            photoData.getOriginalName(),
            photoData.getPath(),
            photoData.getCapturedAt(),
            photoData.getCapacity(),
            photoData.getCreatedAt() != null ? photoData.getCreatedAt() : LocalDateTime.now()
        );
        photo.setId(id); // SpaceContent와 동일한 ID 사용
        return photo;
    }
}