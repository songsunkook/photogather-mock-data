package io.spring.imagegenerator.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "space_content")
public class SpaceContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 16)
    private ContentType contentType;
    
    @Column(name = "space_id", nullable = false)
    private Long spaceId;
    
    @Column(name = "guest_id")
    private Long guestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", insertable = false, updatable = false)
    private Space space;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", insertable = false, updatable = false)
    private Guest guest;
    
    public enum ContentType {
        PHOTO, VIDEO, TEXT
    }
    
    public SpaceContent() {}
    
    public SpaceContent(ContentType contentType, Long spaceId, Long guestId) {
        this.contentType = contentType;
        this.spaceId = spaceId;
        this.guestId = guestId;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }
    
    public Long getSpaceId() { return spaceId; }
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    
    public Long getGuestId() { return guestId; }
    public void setGuestId(Long guestId) { this.guestId = guestId; }
    
    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
}