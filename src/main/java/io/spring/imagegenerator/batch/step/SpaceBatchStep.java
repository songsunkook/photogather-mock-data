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

import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.repository.SpaceRepository;

@Component
public class SpaceBatchStep {

    @Autowired
    private SpaceRepository spaceRepository;

    @Value("${csv.file.path.spaces:spaces.csv}")
    private String spacesFilePath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(spacesFilePath));
        reader.setLinesToSkip(1); // Skip header
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], Space> processor() {
        return fields -> {
            Space space = new Space(
                fields[1], // code
                fields[2].replace("\"", ""), // name
                Integer.parseInt(fields[3]), // valid_hours
                LocalDateTime.parse(fields[4], FORMATTER), // opened_at
                Long.parseLong(fields[5]), // max_capacity
                Space.SpaceType.valueOf(fields[6]), // type
                LocalDateTime.parse(fields[7], FORMATTER), // created_at
                LocalDateTime.parse(fields[8], FORMATTER) // updated_at
            );
            space.setId(Long.parseLong(fields[0]));
            return space;
        };
    }

    public ItemWriter<Space> writer() {
        return new ItemWriter<Space>() {
            @Override
            public void write(Chunk<? extends Space> chunk) throws Exception {
                List<? extends Space> items = chunk.getItems();
                spaceRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  Spaces: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}