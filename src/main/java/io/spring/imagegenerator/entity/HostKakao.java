package io.spring.imagegenerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "host_kakao")
public class HostKakao {
    
    @Id
    private Long id;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private Host host;
    
    public HostKakao() {}
    
    public HostKakao(Long hostId, String userId) {
        this.hostId = hostId;
        this.userId = userId;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Host getHost() { return host; }
    public void setHost(Host host) { this.host = host; }
}
