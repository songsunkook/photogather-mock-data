package io.spring.imagegenerator.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "host")
public class Host {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 100)
    private String name;
    
    @Column(name = "picture_url", length = 255)
    private String pictureUrl;
    
    @Column(name = "agreed_terms", nullable = false)
    private Boolean agreedTerms = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public Host() {}
    
    public Host(String name, String pictureUrl, Boolean agreedTerms, 
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.agreedTerms = agreedTerms;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
}