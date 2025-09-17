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

import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.repository.PhotoRepository;

@Component
public class PhotoBatchStep {

    @Autowired
    private PhotoRepository photoRepository;

    @Value("${csv.file.path.photos:photos.csv}")
    private String photosFilePath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(photosFilePath));
        reader.setLinesToSkip(1);
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], Photo> processor() {
        return fields -> {
            String capturedAtStr = fields[3].trim();
            LocalDateTime capturedAt = null;
            if (!capturedAtStr.isEmpty() && !capturedAtStr.equals("NULL")) {
                capturedAt = LocalDateTime.parse(capturedAtStr, FORMATTER);
            }
            
            Photo photo = new Photo(
                fields[1].replace("\"", ""), // original_name
                fields[2].replace("\"", ""), // path
                capturedAt, // captured_at (nullable)
                Long.parseLong(fields[4]), // capacity
                LocalDateTime.parse(fields[5], FORMATTER) // created_at
            );
            photo.setId(Long.parseLong(fields[0]));
            return photo;
        };
    }

    public ItemWriter<Photo> writer() {
        return new ItemWriter<Photo>() {
            @Override
            public void write(Chunk<? extends Photo> chunk) throws Exception {
                List<? extends Photo> items = chunk.getItems();
                photoRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  Photos: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}