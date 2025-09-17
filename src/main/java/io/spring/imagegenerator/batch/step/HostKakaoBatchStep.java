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

import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.repository.HostKakaoRepository;

@Component
public class HostKakaoBatchStep {

    @Autowired
    private HostKakaoRepository hostKakaoRepository;

    @Value("${csv.file.path.hostKakaos:host_kakaos.csv}")
    private String hostKakaosFilePath;
    
    private long processedCount = 0;
    private static final long REPORT_INTERVAL = 10000;

    public ItemReader<String[]> reader() {
        FlatFileItemReader<String[]> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(hostKakaosFilePath));
        reader.setLinesToSkip(1);
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        
        DefaultLineMapper<String[]> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> fieldSet.getValues());
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    public ItemProcessor<String[], HostKakao> processor() {
        return fields -> {
            HostKakao hostKakao = new HostKakao(
                Long.parseLong(fields[1]), // host_id
                fields[2] // user_id
            );
            hostKakao.setId(Long.parseLong(fields[0]));
            return hostKakao;
        };
    }

    public ItemWriter<HostKakao> writer() {
        return new ItemWriter<HostKakao>() {
            @Override
            public void write(Chunk<? extends HostKakao> chunk) throws Exception {
                List<? extends HostKakao> items = chunk.getItems();
                hostKakaoRepository.saveAll(items);
                
                processedCount += items.size();
                
                // 10000개 단위로 진행률 출력
                if (processedCount % REPORT_INTERVAL == 0 || 
                    (processedCount % REPORT_INTERVAL < items.size() && processedCount >= REPORT_INTERVAL)) {
                    long reportCount = (processedCount / REPORT_INTERVAL) * REPORT_INTERVAL;
                    if (reportCount > 0) {
                        System.out.printf("  HostKakaos: Processed %d records\n", reportCount);
                    }
                }
            }
        };
    }
}