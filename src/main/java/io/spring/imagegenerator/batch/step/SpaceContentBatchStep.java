package io.spring.imagegenerator.batch.step;

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

import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.repository.SpaceContentRepository;

@Component
public class SpaceContentBatchStep {

    @Autowired
    private SpaceContentRepository spaceContentRepository;

    @Value("${csv.file.path.spaceContents:space_contents.csv}")
    private String spaceContentsFilePath;
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(spaceContentsFilePath));
        reader.setLinesToSkip(1);
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], SpaceContent> processor() {
        return fields -> {
            String guestIdStr = fields[3].trim();
            Long guestId = null;
            if (!guestIdStr.isEmpty() && !guestIdStr.equals("NULL")) {
                guestId = Long.parseLong(guestIdStr);
            }
            
            SpaceContent spaceContent = new SpaceContent(
                SpaceContent.ContentType.valueOf(fields[1]), // content_type
                Long.parseLong(fields[2]), // space_id
                guestId // guest_id (nullable)
            );
            spaceContent.setId(Long.parseLong(fields[0]));
            return spaceContent;
        };
    }

    public ItemWriter<SpaceContent> writer() {
        return new ItemWriter<SpaceContent>() {
            @Override
            public void write(Chunk<? extends SpaceContent> chunk) throws Exception {
                List<? extends SpaceContent> items = chunk.getItems();
                spaceContentRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  SpaceContents: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}