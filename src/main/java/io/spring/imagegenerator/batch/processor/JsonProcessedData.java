package io.spring.imagegenerator.batch.processor;

import java.util.List;

import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

public class JsonProcessedData {
    
    private Space space;
    private Host host;
    private SpaceHostMap spaceHostMap;
    private HostKakao hostKakao;
    private List<Guest> guests;
    private List<SpaceContent> spaceContents;
    private List<Photo> photos;
    
    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    
    public Host getHost() { return host; }
    public void setHost(Host host) { this.host = host; }
    
    public SpaceHostMap getSpaceHostMap() { return spaceHostMap; }
    public void setSpaceHostMap(SpaceHostMap spaceHostMap) { this.spaceHostMap = spaceHostMap; }
    
    public HostKakao getHostKakao() { return hostKakao; }
    public void setHostKakao(HostKakao hostKakao) { this.hostKakao = hostKakao; }
    
    public List<Guest> getGuests() { return guests; }
    public void setGuests(List<Guest> guests) { this.guests = guests; }
    
    public List<SpaceContent> getSpaceContents() { return spaceContents; }
    public void setSpaceContents(List<SpaceContent> spaceContents) { this.spaceContents = spaceContents; }
    
    public List<Photo> getPhotos() { return photos; }
    public void setPhotos(List<Photo> photos) { this.photos = photos; }
}