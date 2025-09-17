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

import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.repository.HostRepository;

@Component
public class HostBatchStep {

    @Autowired
    private HostRepository hostRepository;

    @Value("${csv.file.path.hosts:hosts.csv}")
    private String hostsFilePath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(hostsFilePath));
        reader.setLinesToSkip(1); // Skip header
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], Host> processor() {
        return fields -> {
            String name = fields[1].replace("\"", "").trim();
            if (name.isEmpty()) {
                name = null;
            }
            
            String pictureUrl = fields[2].trim();
            if (pictureUrl.isEmpty() || pictureUrl.equals("NULL")) {
                pictureUrl = null;
            }
            
            Host host = new Host(
                name, // name (nullable)
                pictureUrl, // picture_url (nullable)
                Boolean.parseBoolean(fields[3]), // agreed_terms
                LocalDateTime.parse(fields[4], FORMATTER), // created_at
                LocalDateTime.parse(fields[5], FORMATTER) // updated_at
            );
            host.setId(Long.parseLong(fields[0]));
            return host;
        };
    }

    public ItemWriter<Host> writer() {
        return new ItemWriter<Host>() {
            @Override
            public void write(Chunk<? extends Host> chunk) throws Exception {
                List<? extends Host> items = chunk.getItems();
                hostRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  Hosts: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}