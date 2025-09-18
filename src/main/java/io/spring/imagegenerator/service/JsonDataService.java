package io.spring.imagegenerator.service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.spring.imagegenerator.dto.JsonSpaceData;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;
import io.spring.imagegenerator.repository.GuestRepository;
import io.spring.imagegenerator.repository.HostKakaoRepository;
import io.spring.imagegenerator.repository.HostRepository;
import io.spring.imagegenerator.repository.PhotoRepository;
import io.spring.imagegenerator.repository.SpaceContentRepository;
import io.spring.imagegenerator.repository.SpaceHostMapRepository;
import io.spring.imagegenerator.repository.SpaceRepository;

@Service
public class JsonDataService {

    @Autowired
    private SpaceRepository spaceRepository;
    
    @Autowired
    private HostRepository hostRepository;
    
    @Autowired
    private SpaceHostMapRepository spaceHostMapRepository;
    
    @Autowired
    private HostKakaoRepository hostKakaoRepository;
    
    @Autowired
    private GuestRepository guestRepository;
    
    @Autowired
    private SpaceContentRepository spaceContentRepository;
    
    @Autowired
    private PhotoRepository photoRepository;

    private final ObjectMapper objectMapper;
    
    public JsonDataService() {
        this.objectMapper = new ObjectMapper();
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        
        this.objectMapper.registerModule(javaTimeModule);
    }

    @Transactional
    public void loadSpaceFromJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            JsonSpaceData jsonData = objectMapper.readValue(reader, JsonSpaceData.class);
            
            List<JsonSpaceData.SpaceData> spacesToProcess = new ArrayList<>();
            
            if (jsonData.getSpace() != null) {
                spacesToProcess.add(jsonData.getSpace());
            }
            
            if (jsonData.getSpaces() != null) {
                spacesToProcess.addAll(jsonData.getSpaces());
            }
            
            for (JsonSpaceData.SpaceData spaceData : spacesToProcess) {
                processSpaceData(spaceData);
            }
            
            System.out.printf("Successfully processed %d space(s) from JSON file\n", spacesToProcess.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load space from JSON", e);
        }
    }
    
    private void processSpaceData(JsonSpaceData.SpaceData spaceData) {
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
        space = spaceRepository.save(space);
        System.out.printf("Space created with ID: %d\n", space.getId());
        
        if (spaceData.getHost() != null) {
            Host host = createHost(spaceData.getHost());
            host = hostRepository.save(host);
            System.out.printf("Host created with ID: %d\n", host.getId());
            
            SpaceHostMap spaceHostMap = new SpaceHostMap(
                space.getId(),
                host.getId(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            spaceHostMapRepository.save(spaceHostMap);
            System.out.printf("SpaceHostMap created\n");
            
            if (spaceData.getHost().getKakao() != null) {
                HostKakao hostKakao = new HostKakao(
                    host.getId(),
                    spaceData.getHost().getKakao().getUserId()
                );
                hostKakaoRepository.save(hostKakao);
                System.out.printf("HostKakao created\n");
            }
        }
        
        if (spaceData.getGuests() != null) {
            for (JsonSpaceData.GuestData guestData : spaceData.getGuests()) {
                Guest guest = new Guest(
                    space.getId(),
                    guestData.getName(),
                    guestData.getCreatedAt(),
                    guestData.getUpdatedAt()
                );
                guest = guestRepository.save(guest);
                System.out.printf("Guest created with ID: %d\n", guest.getId());
                
                if (guestData.getPhotos() != null) {
                    for (JsonSpaceData.PhotoData photoData : guestData.getPhotos()) {
                        SpaceContent spaceContent = new SpaceContent(
                            SpaceContent.ContentType.valueOf(photoData.getContentType()),
                            space.getId(),
                            guest.getId()
                        );
                        spaceContent = spaceContentRepository.save(spaceContent);
                        System.out.printf("SpaceContent created with ID: %d\n", spaceContent.getId());
                        
                        Photo photo = new Photo(
                            photoData.getOriginalName(),
                            photoData.getPath(),
                            photoData.getCapturedAt(),
                            photoData.getCapacity(),
                            photoData.getCreatedAt()
                        );
                        photo.setId(spaceContent.getId());
                        photo = photoRepository.save(photo);
                        System.out.printf("Photo created with ID: %d\n", photo.getId());
                    }
                }
            }
        }
    }
    
    private Host createHost(JsonSpaceData.HostData hostData) {
        return new Host(
            hostData.getName(),
            hostData.getPictureUrl(),
            hostData.getAgreedTerms(),
            hostData.getCreatedAt(),
            hostData.getUpdatedAt()
        );
    }
}
