package io.spring.imagegenerator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "space_host_map")
public class SpaceHostMap {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "space_id", nullable = false)
    private Long spaceId;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", insertable = false, updatable = false)
    private Space space;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private Host host;
    
    public SpaceHostMap() {}
    
    public SpaceHostMap(Long spaceId, Long hostId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.spaceId = spaceId;
        this.hostId = hostId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSpaceId() { return spaceId; }
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    
    public Host getHost() { return host; }
    public void setHost(Host host) { this.host = host; }
}
