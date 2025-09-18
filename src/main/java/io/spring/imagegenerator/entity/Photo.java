package io.spring.imagegenerator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "photo")
public class Photo {
    
    @Id
    private Long id;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    
    @Column(nullable = false, length = 255)
    private String path;
    
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
    
    @Column(nullable = false)
    private Long capacity;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    @MapsId
    private SpaceContent spaceContent;
    
    public Photo() {}
    
    public Photo(String originalName, String path, LocalDateTime capturedAt, 
                 Long capacity, LocalDateTime createdAt) {
        this.originalName = originalName;
        this.path = path;
        this.capturedAt = capturedAt;
        this.capacity = capacity;
        this.createdAt = createdAt;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public SpaceContent getSpaceContent() { return spaceContent; }
    public void setSpaceContent(SpaceContent spaceContent) { this.spaceContent = spaceContent; }
}
