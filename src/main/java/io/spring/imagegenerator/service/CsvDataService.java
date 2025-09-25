package io.spring.imagegenerator.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CsvDataService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public List<Space> loadSpacesFromCsv(CSVReader reader, int limit) {
        List<Space> spaces = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && spaces.size() < limit) {
                Space space = new Space(
                    record[0],
                    record[1].replace("\"", ""),
                    Integer.parseInt(record[2]),
                    LocalDateTime.parse(record[3], FORMATTER),
                    Long.parseLong(record[4]),
                    Space.SpaceType.valueOf(record[5]),
                    LocalDateTime.parse(record[6], FORMATTER),
                    LocalDateTime.parse(record[7], FORMATTER)
                );
                spaces.add(space);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load spaces from CSV", e);
        }
        return spaces;
    }
    

    public List<Host> loadHostsFromCsv(CSVReader reader, int limit) {
        List<Host> hosts = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && hosts.size() < limit) {
                Host host = new Host(
                    isNullOrEmpty(record[0]) ? null : record[0].replace("\"", ""),
                    isNullOrEmpty(record[1]) ? null : record[1],
                    Boolean.parseBoolean(record[2]),
                    LocalDateTime.parse(record[3], FORMATTER),
                    LocalDateTime.parse(record[4], FORMATTER)
                );
                hosts.add(host);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load hosts from CSV", e);
        }
        return hosts;
    }

    public List<SpaceHostMap> loadSpaceHostMapsFromCsv(CSVReader reader, int limit) {
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && spaceHostMaps.size() < limit) {
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    Long.parseLong(record[0]),
                    Long.parseLong(record[1]),
                    isNullOrEmpty(record[2]) ? null : LocalDateTime.parse(record[2], FORMATTER),
                    isNullOrEmpty(record[3]) ? null : LocalDateTime.parse(record[3], FORMATTER)
                );
                spaceHostMaps.add(spaceHostMap);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space host maps from CSV", e);
        }
        return spaceHostMaps;
    }

    public List<HostKakao> loadHostKakaosFromCsv(CSVReader reader, int limit) {
        List<HostKakao> hostKakaos = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && hostKakaos.size() < limit) {
                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[0]),
                    record[1]
                );
                hostKakaos.add(hostKakao);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load host kakaos from CSV", e);
        }
        return hostKakaos;
    }

    public List<Guest> loadGuestsFromCsv(CSVReader reader, int limit) {
        List<Guest> guests = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && guests.size() < limit) {
                Guest guest = new Guest(
                    Long.parseLong(record[0]),
                    isNullOrEmpty(record[1]) ? null : record[1].replace("\"", ""),
                    LocalDateTime.parse(record[2], FORMATTER),
                    LocalDateTime.parse(record[3], FORMATTER)
                );
                guests.add(guest);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load guests from CSV", e);
        }
        return guests;
    }

    public List<SpaceContent> loadSpaceContentsFromCsv(CSVReader reader, int limit) {
        List<SpaceContent> spaceContents = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && spaceContents.size() < limit) {
                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[0]),
                    Long.parseLong(record[1]),
                    isNullOrEmpty(record[2]) ? null : Long.parseLong(record[2])
                );
                spaceContents.add(spaceContent);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load space contents from CSV", e);
        }
        return spaceContents;
    }

    public List<Photo> loadPhotosFromCsv(CSVReader reader, int limit) {
        List<Photo> photos = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null && photos.size() < limit) {
                Photo photo = new Photo(
                    record[0].replace("\"", ""),
                    record[1].replace("\"", ""),
                    isNullOrEmpty(record[2]) ? null : LocalDateTime.parse(record[2], FORMATTER),
                    Long.parseLong(record[3]),
                    LocalDateTime.parse(record[4], FORMATTER)
                );
                photos.add(photo);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load photos from CSV", e);
        }
        return photos;
    }

    // Methods to load all data without limit
    public List<Space> loadAllSpacesFromCsv(CSVReader reader) {
        List<Space> spaces = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                Space space = new Space(
                    record[0],
                    record[1].replace("\"", ""),
                    Integer.parseInt(record[2]),
                    LocalDateTime.parse(record[3], FORMATTER),
                    Long.parseLong(record[4]),
                    Space.SpaceType.valueOf(record[5]),
                    LocalDateTime.parse(record[6], FORMATTER),
                    LocalDateTime.parse(record[7], FORMATTER)
                );
                spaces.add(space);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all spaces from CSV", e);
        }
        return spaces;
    }

    public List<Host> loadAllHostsFromCsv(CSVReader reader) {
        List<Host> hosts = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                Host host = new Host(
                    isNullOrEmpty(record[0]) ? null : record[0].replace("\"", ""),
                    isNullOrEmpty(record[1]) ? null : record[1],
                    Boolean.parseBoolean(record[2]),
                    LocalDateTime.parse(record[3], FORMATTER),
                    LocalDateTime.parse(record[4], FORMATTER)
                );
                hosts.add(host);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all hosts from CSV", e);
        }
        return hosts;
    }

    public List<SpaceHostMap> loadAllSpaceHostMapsFromCsv(CSVReader reader) {
        List<SpaceHostMap> spaceHostMaps = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                SpaceHostMap spaceHostMap = new SpaceHostMap(
                    Long.parseLong(record[0]),
                    Long.parseLong(record[1]),
                    isNullOrEmpty(record[2]) ? null : LocalDateTime.parse(record[2], FORMATTER),
                    isNullOrEmpty(record[3]) ? null : LocalDateTime.parse(record[3], FORMATTER)
                );
                spaceHostMaps.add(spaceHostMap);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all space host maps from CSV", e);
        }
        return spaceHostMaps;
    }

    public List<HostKakao> loadAllHostKakaosFromCsv(CSVReader reader) {
        List<HostKakao> hostKakaos = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                HostKakao hostKakao = new HostKakao(
                    Long.parseLong(record[0]),
                    record[1]
                );
                hostKakaos.add(hostKakao);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all host kakaos from CSV", e);
        }
        return hostKakaos;
    }

    public List<Guest> loadAllGuestsFromCsv(CSVReader reader) {
        List<Guest> guests = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                Guest guest = new Guest(
                    Long.parseLong(record[0]),
                    isNullOrEmpty(record[1]) ? null : record[1].replace("\"", ""),
                    LocalDateTime.parse(record[2], FORMATTER),
                    LocalDateTime.parse(record[3], FORMATTER)
                );
                guests.add(guest);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all guests from CSV", e);
        }
        return guests;
    }

    public List<SpaceContent> loadAllSpaceContentsFromCsv(CSVReader reader) {
        List<SpaceContent> spaceContents = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                SpaceContent spaceContent = new SpaceContent(
                    SpaceContent.ContentType.valueOf(record[0]),
                    Long.parseLong(record[1]),
                    isNullOrEmpty(record[2]) ? null : Long.parseLong(record[2])
                );
                spaceContents.add(spaceContent);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all space contents from CSV", e);
        }
        return spaceContents;
    }

    public List<Photo> loadAllPhotosFromCsv(CSVReader reader) {
        List<Photo> photos = new ArrayList<>();
        try {
            String[] record;
            while ((record = reader.readNext()) != null) {
                Photo photo = new Photo(
                    record[0].replace("\"", ""),
                    record[1].replace("\"", ""),
                    isNullOrEmpty(record[2]) ? null : LocalDateTime.parse(record[2], FORMATTER),
                    Long.parseLong(record[3]),
                    LocalDateTime.parse(record[4], FORMATTER)
                );
                photos.add(photo);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load all photos from CSV", e);
        }
        return photos;
    }
}