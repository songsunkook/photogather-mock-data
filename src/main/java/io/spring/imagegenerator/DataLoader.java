package io.spring.imagegenerator;

import com.opencsv.CSVReader;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;
import io.spring.imagegenerator.service.CsvDataService;
import io.spring.imagegenerator.service.JdbcBulkInsertService;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private CsvDataService csvDataService;

    @Autowired
    private JdbcBulkInsertService jdbcBulkInsertService;

    @Value("${csv.data.path}")
    private String csvDataPath;

    @Override
    public void run(String... args) throws Exception {
        long totalStartTime = System.currentTimeMillis();
        System.out.println("Starting sequential cyclic CSV data loading process...");

        AtomicLong totalSpaces = new AtomicLong(0);
        AtomicLong totalHosts = new AtomicLong(0);
        AtomicLong totalSpaceHostMaps = new AtomicLong(0);
        AtomicLong totalHostKakaos = new AtomicLong(0);
        AtomicLong totalGuests = new AtomicLong(0);
        AtomicLong totalSpaceContents = new AtomicLong(0);
        AtomicLong totalPhotos = new AtomicLong(0);

        try (
            CSVReader spaceReader = new CSVReader(new FileReader(csvDataPath + "/1_space.csv"));
            CSVReader hostReader = new CSVReader(new FileReader(csvDataPath + "/2_host.csv"));
            CSVReader spaceHostMapReader = new CSVReader(new FileReader(csvDataPath + "/3_space_host_map.csv"));
            CSVReader hostKakaoReader = new CSVReader(new FileReader(csvDataPath + "/4_host_kakao.csv"));
            CSVReader guestReader = new CSVReader(new FileReader(csvDataPath + "/5_guest.csv"));
            CSVReader spaceContentReader = new CSVReader(new FileReader(csvDataPath + "/6_space_content.csv"));
            CSVReader photoReader = new CSVReader(new FileReader(csvDataPath + "/7_photo.csv"))
        ) {
            // Skip headers
            spaceReader.readNext();
            hostReader.readNext();
            spaceHostMapReader.readNext();
            hostKakaoReader.readNext();
            guestReader.readNext();
            spaceContentReader.readNext();
            photoReader.readNext();

            int cycle = 0;
            while (true) {
                cycle++;
                long cycleStartTime = System.currentTimeMillis();
                boolean dataLoadedInCycle = false;
                StringBuilder cycleLog = new StringBuilder();

                // 1. Spaces (100) - 순차적으로 먼저 삽입
                List<Space> spaces = csvDataService.loadSpacesFromCsv(spaceReader, 100);
                if (!spaces.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertSpaces(spaces);
                    cycleLog.append(String.format("Spaces: %d/%d | ", spaces.size(), totalSpaces.addAndGet(spaces.size())));
                }

                // 2. Hosts (100) - 두 번째로 삽입
                List<Host> hosts = csvDataService.loadHostsFromCsv(hostReader, 100);
                if (!hosts.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertHosts(hosts);
                    cycleLog.append(String.format("Hosts: %d/%d | ", hosts.size(), totalHosts.addAndGet(hosts.size())));
                }

                // 3. SpaceHostMaps (100) - Space와 Host가 있어야 삽입 가능
                List<SpaceHostMap> spaceHostMaps = csvDataService.loadSpaceHostMapsFromCsv(spaceHostMapReader, 100);
                if (!spaceHostMaps.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertSpaceHostMaps(spaceHostMaps);
                    cycleLog.append(String.format("SpaceHostMaps: %d/%d | ", spaceHostMaps.size(), totalSpaceHostMaps.addAndGet(spaceHostMaps.size())));
                }

                // 4. HostKakaos (100) - Host가 있어야 삽입 가능
                List<HostKakao> hostKakaos = csvDataService.loadHostKakaosFromCsv(hostKakaoReader, 100);
                if (!hostKakaos.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertHostKakaos(hostKakaos);
                    cycleLog.append(String.format("HostKakaos: %d/%d | ", hostKakaos.size(), totalHostKakaos.addAndGet(hostKakaos.size())));
                }

                // 5. Guests (1000) - Space가 있어야 삽입 가능
                List<Guest> guests = csvDataService.loadGuestsFromCsv(guestReader, 1000);
                if (!guests.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertGuests(guests);
                    cycleLog.append(String.format("Guests: %d/%d | ", guests.size(), totalGuests.addAndGet(guests.size())));
                }

                // 6. SpaceContents (20000) - Space와 Guest가 있어야 삽입 가능
                List<SpaceContent> spaceContents = csvDataService.loadSpaceContentsFromCsv(spaceContentReader, 20000);
                if (!spaceContents.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertSpaceContents(spaceContents);
                    cycleLog.append(String.format("SpaceContents: %d/%d | ", spaceContents.size(), totalSpaceContents.addAndGet(spaceContents.size())));
                }

                // 7. Photos (20000) - 마지막에 삽입
                List<Photo> photos = csvDataService.loadPhotosFromCsv(photoReader, 20000);
                if (!photos.isEmpty()) {
                    dataLoadedInCycle = true;
                    jdbcBulkInsertService.bulkInsertPhotos(photos);
                    cycleLog.append(String.format("Photos: %d/%d | ", photos.size(), totalPhotos.addAndGet(photos.size())));
                }

                if (!dataLoadedInCycle) {
                    System.out.println("\n--- All files have been read. Finishing process. ---");
                    break;
                }

                long cycleDuration = System.currentTimeMillis() - cycleStartTime;
                if (cycleLog.length() > 0) {
                    cycleLog.setLength(cycleLog.length() - 3);
                }
                System.out.printf("Cycle %d (%dms): [ %s ]\n", cycle, cycleDuration, cycleLog.toString());
            }
        }

        long totalEndTime = System.currentTimeMillis();
        System.out.println("\n--------------------------------------------------");
        System.out.println("All CSV data loaded successfully!");
        System.out.println("Total time: " + (totalEndTime - totalStartTime) + "ms");
        System.out.println("--------------------------------------------------");
        System.out.printf("Total inserted rows:\n" +
            "- Spaces: %d\n" +
            "- Hosts: %d\n" +
            "- SpaceHostMaps: %d\n" +
            "- HostKakaos: %d\n" +
            "- Guests: %d\n" +
            "- SpaceContents: %d\n" +
            "- Photos: %d\n",
            totalSpaces.get(), totalHosts.get(), totalSpaceHostMaps.get(), totalHostKakaos.get(),
            totalGuests.get(), totalSpaceContents.get(), totalPhotos.get());
        System.out.println("--------------------------------------------------");
    }
}