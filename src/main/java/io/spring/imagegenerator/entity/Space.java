package io.spring.imagegenerator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "space")
public class Space {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 64)
    private String code;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(name = "valid_hours", nullable = false)
    private Integer validHours;
    
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;
    
    @Column(name = "max_capacity", nullable = false)
    private Long maxCapacity = 10737418240L;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 45)
    private SpaceType type = SpaceType.PRIVATE;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum SpaceType {
        PRIVATE, PUBLIC
    }
    
    public Space() {}
    
    public Space(String code, String name, Integer validHours, LocalDateTime openedAt, 
                 Long maxCapacity, SpaceType type, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.code = code;
        this.name = name;
        this.validHours = validHours;
        this.openedAt = openedAt;
        this.maxCapacity = maxCapacity;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public SpaceType getType() { return type; }
    public void setType(SpaceType type) { this.type = type; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
