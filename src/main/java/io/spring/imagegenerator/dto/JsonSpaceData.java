package io.spring.imagegenerator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class JsonSpaceData {
    private SpaceData space;
    private List<SpaceData> spaces;
    
    public SpaceData getSpace() { return space; }
    public void setSpace(SpaceData space) { this.space = space; }
    
    public List<SpaceData> getSpaces() { return spaces; }
    public void setSpaces(List<SpaceData> spaces) { this.spaces = spaces; }
    
    public static class SpaceData {
        private String code;
        private String name;
        @JsonProperty("valid_hours")
        private Integer validHours;
        @JsonProperty("opened_at")
        private LocalDateTime openedAt;
        @JsonProperty("max_capacity")
        private Long maxCapacity;
        private String type;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
        private HostData host;
        private List<GuestData> guests;
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Integer getValidHours() { return validHours; }
        public void setValidHours(Integer validHours) { this.validHours = validHours; }
        
        public LocalDateTime getOpenedAt() { return openedAt; }
        public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
        
        public Long getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Long maxCapacity) { this.maxCapacity = maxCapacity; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        
        public HostData getHost() { return host; }
        public void setHost(HostData host) { this.host = host; }
        
        public List<GuestData> getGuests() { return guests; }
        public void setGuests(List<GuestData> guests) { this.guests = guests; }
    }
    
    public static class HostData {
        private String name;
        @JsonProperty("picture_url")
        private String pictureUrl;
        @JsonProperty("agreed_terms")
        private Boolean agreedTerms;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
        private KakaoData kakao;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPictureUrl() { return pictureUrl; }
        public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
        
        public Boolean getAgreedTerms() { return agreedTerms; }
        public void setAgreedTerms(Boolean agreedTerms) { this.agreedTerms = agreedTerms; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        
        public KakaoData getKakao() { return kakao; }
        public void setKakao(KakaoData kakao) { this.kakao = kakao; }
    }
    
    public static class KakaoData {
        @JsonProperty("user_id")
        private String userId;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
    
    public static class GuestData {
        private String name;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
        private List<PhotoData> photos;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        
        public List<PhotoData> getPhotos() { return photos; }
        public void setPhotos(List<PhotoData> photos) { this.photos = photos; }
    }
    
    public static class PhotoData {
        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("original_name")
        private String originalName;
        private String path;
        @JsonProperty("captured_at")
        private LocalDateTime capturedAt;
        private Long capacity;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public LocalDateTime getCapturedAt() { return capturedAt; }
        public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
        
        public Long getCapacity() { return capacity; }
        public void setCapacity(Long capacity) { this.capacity = capacity; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}