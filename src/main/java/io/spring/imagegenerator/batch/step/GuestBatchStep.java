package io.spring.imagegenerator.batch.step;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.repository.GuestRepository;

@Component
public class GuestBatchStep {

    @Autowired
    private GuestRepository guestRepository;

    @Value("${csv.file.path.guests:guests.csv}")
    private String guestsFilePath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(guestsFilePath));
        reader.setLinesToSkip(1);
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], Guest> processor() {
        return fields -> {
            String name = fields[2].replace("\"", "").trim();
            if (name.isEmpty()) {
                name = null;
            }
            
            Guest guest = new Guest(
                Long.parseLong(fields[1]), // space_id
                name, // name (nullable)
                LocalDateTime.parse(fields[3], FORMATTER), // created_at
                LocalDateTime.parse(fields[4], FORMATTER) // updated_at
            );
            guest.setId(Long.parseLong(fields[0]));
            return guest;
        };
    }

    public ItemWriter<Guest> writer() {
        return new ItemWriter<Guest>() {
            @Override
            public void write(Chunk<? extends Guest> chunk) throws Exception {
                List<? extends Guest> items = chunk.getItems();
                guestRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  Guests: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}