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

import io.spring.imagegenerator.entity.SpaceHostMap;
import io.spring.imagegenerator.repository.SpaceHostMapRepository;

@Component
public class SpaceHostMapBatchStep {

    @Autowired
    private SpaceHostMapRepository spaceHostMapRepository;

    @Value("${csv.file.path.spaceHostMaps:space_host_maps.csv}")
    private String spaceHostMapsFilePath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(spaceHostMapsFilePath));
        reader.setLinesToSkip(1);
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], SpaceHostMap> processor() {
        return fields -> {
            SpaceHostMap spaceHostMap = new SpaceHostMap(
                Long.parseLong(fields[1]), // space_id
                Long.parseLong(fields[2]), // host_id
                LocalDateTime.parse(fields[3], FORMATTER), // created_at
                LocalDateTime.parse(fields[4], FORMATTER) // updated_at
            );
            spaceHostMap.setId(Long.parseLong(fields[0]));
            return spaceHostMap;
        };
    }

    public ItemWriter<SpaceHostMap> writer() {
        return new ItemWriter<SpaceHostMap>() {
            @Override
            public void write(Chunk<? extends SpaceHostMap> chunk) throws Exception {
                List<? extends SpaceHostMap> items = chunk.getItems();
                spaceHostMapRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  SpaceHostMaps: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}