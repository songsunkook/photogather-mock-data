package io.spring.imagegenerator.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import io.spring.imagegenerator.batch.processor.JsonProcessedData;
import io.spring.imagegenerator.batch.processor.JsonSpaceProcessor;
import io.spring.imagegenerator.batch.reader.JsonSpaceItemReader;
import io.spring.imagegenerator.batch.step.GuestBatchStep;
import io.spring.imagegenerator.batch.step.HostBatchStep;
import io.spring.imagegenerator.batch.step.HostKakaoBatchStep;
import io.spring.imagegenerator.batch.step.PhotoBatchStep;
import io.spring.imagegenerator.batch.step.SpaceBatchStep;
import io.spring.imagegenerator.batch.step.SpaceContentBatchStep;
import io.spring.imagegenerator.batch.step.SpaceHostMapBatchStep;
import io.spring.imagegenerator.batch.writer.JsonDataWriter;
import io.spring.imagegenerator.dto.JsonSpaceData;
import io.spring.imagegenerator.entity.Guest;
import io.spring.imagegenerator.entity.Host;
import io.spring.imagegenerator.entity.HostKakao;
import io.spring.imagegenerator.entity.Photo;
import io.spring.imagegenerator.entity.Space;
import io.spring.imagegenerator.entity.SpaceContent;
import io.spring.imagegenerator.entity.SpaceHostMap;

@Configuration
public class BatchConfiguration {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SpaceBatchStep spaceBatchStep;

    @Autowired
    private HostBatchStep hostBatchStep;

    @Autowired
    private SpaceHostMapBatchStep spaceHostMapBatchStep;

    @Autowired
    private HostKakaoBatchStep hostKakaoBatchStep;

    @Autowired
    private GuestBatchStep guestBatchStep;

    @Autowired
    private SpaceContentBatchStep spaceContentBatchStep;

    @Autowired
    private PhotoBatchStep photoBatchStep;

    @Autowired
    private JsonSpaceProcessor jsonSpaceProcessor;

    @Autowired
    private JsonDataWriter jsonDataWriter;
    
    @Value("${json.file.path}")
    private String jsonFilePath;

    // JdbcBatchItemWriter Beans with Progress Reporting
    @Bean
    public ItemWriter<Space> spaceWriter() {
        JdbcBatchItemWriter<Space> jdbcWriter = new JdbcBatchItemWriterBuilder<Space>()
                .dataSource(dataSource)
                .sql("INSERT INTO space (id, code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter((space, ps) -> {
                    ps.setLong(1, space.getId());
                    ps.setString(2, space.getCode());
                    ps.setString(3, space.getName());
                    ps.setInt(4, space.getValidHours());
                    ps.setTimestamp(5, java.sql.Timestamp.valueOf(space.getOpenedAt()));
                    ps.setLong(6, space.getMaxCapacity());
                    ps.setString(7, space.getType().name());
                    ps.setTimestamp(8, java.sql.Timestamp.valueOf(space.getCreatedAt()));
                    ps.setTimestamp(9, java.sql.Timestamp.valueOf(space.getUpdatedAt()));
                })
                .build();
        
        return new ProgressReportingWriter<>("Spaces", jdbcWriter, 10000);
    }

    @Bean
    public ItemWriter<Host> hostWriter() {
        JdbcBatchItemWriter<Host> jdbcWriter = new JdbcBatchItemWriterBuilder<Host>()
                .dataSource(dataSource)
                .sql("INSERT INTO host (id, name, picture_url, agreed_terms, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter((host, ps) -> {
                    ps.setLong(1, host.getId());
                    if (host.getName() != null) {
                        ps.setString(2, host.getName());
                    } else {
                        ps.setNull(2, java.sql.Types.VARCHAR);
                    }
                    if (host.getPictureUrl() != null) {
                        ps.setString(3, host.getPictureUrl());
                    } else {
                        ps.setNull(3, java.sql.Types.VARCHAR);
                    }
                    ps.setBoolean(4, host.getAgreedTerms());
                    ps.setTimestamp(5, java.sql.Timestamp.valueOf(host.getCreatedAt()));
                    ps.setTimestamp(6, java.sql.Timestamp.valueOf(host.getUpdatedAt()));
                })
                .build();
        
        return new ProgressReportingWriter<>("Hosts", jdbcWriter, 10000);
    }

    @Bean
    public ItemWriter<SpaceHostMap> spaceHostMapWriter() {
        JdbcBatchItemWriter<SpaceHostMap> jdbcWriter = new JdbcBatchItemWriterBuilder<SpaceHostMap>()
                .dataSource(dataSource)
                .sql("INSERT INTO space_host_map (id, space_id, host_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter((spaceHostMap, ps) -> {
                    ps.setLong(1, spaceHostMap.getId());
                    ps.setLong(2, spaceHostMap.getSpaceId());
                    ps.setLong(3, spaceHostMap.getHostId());
                    if (spaceHostMap.getCreatedAt() != null) {
                        ps.setTimestamp(4, java.sql.Timestamp.valueOf(spaceHostMap.getCreatedAt()));
                    } else {
                        ps.setNull(4, java.sql.Types.TIMESTAMP);
                    }
                    if (spaceHostMap.getUpdatedAt() != null) {
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(spaceHostMap.getUpdatedAt()));
                    } else {
                        ps.setNull(5, java.sql.Types.TIMESTAMP);
                    }
                })
                .build();
        
        return new ProgressReportingWriter<>("SpaceHostMaps", jdbcWriter, 10000);
    }

    @Bean
    public ItemWriter<HostKakao> hostKakaoWriter() {
        JdbcBatchItemWriter<HostKakao> jdbcWriter = new JdbcBatchItemWriterBuilder<HostKakao>()
                .dataSource(dataSource)
                .sql("INSERT INTO host_kakao (id, host_id, user_id) VALUES (?, ?, ?)")
                .itemPreparedStatementSetter((hostKakao, ps) -> {
                    ps.setLong(1, hostKakao.getId());
                    ps.setLong(2, hostKakao.getHostId());
                    ps.setString(3, hostKakao.getUserId());
                })
                .build();
        
        return new ProgressReportingWriter<>("HostKakaos", jdbcWriter, 10000);
    }

    @Bean
    public ItemWriter<Guest> guestWriter() {
        JdbcBatchItemWriter<Guest> jdbcWriter = new JdbcBatchItemWriterBuilder<Guest>()
                .dataSource(dataSource)
                .sql("INSERT INTO guest (id, space_id, name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter((guest, ps) -> {
                    ps.setLong(1, guest.getId());
                    ps.setLong(2, guest.getSpaceId());
                    if (guest.getName() != null) {
                        ps.setString(3, guest.getName());
                    } else {
                        ps.setNull(3, java.sql.Types.VARCHAR);
                    }
                    ps.setTimestamp(4, java.sql.Timestamp.valueOf(guest.getCreatedAt()));
                    ps.setTimestamp(5, java.sql.Timestamp.valueOf(guest.getUpdatedAt()));
                })
                .build();
        
        return new ProgressReportingWriter<>("Guests", jdbcWriter, 10000);
    }

    @Bean
    public ItemWriter<SpaceContent> spaceContentWriter() {
        JdbcBatchItemWriter<SpaceContent> jdbcWriter = new JdbcBatchItemWriterBuilder<SpaceContent>()
                .dataSource(dataSource)
                .sql("INSERT INTO space_content (id, content_type, space_id, guest_id) VALUES (?, ?, ?, ?)")
                .itemPreparedStatementSetter((spaceContent, ps) -> {
                    ps.setLong(1, spaceContent.getId());
                    ps.setString(2, spaceContent.getContentType().name());
                    ps.setLong(3, spaceContent.getSpaceId());
                    if (spaceContent.getGuestId() != null) {
                        ps.setLong(4, spaceContent.getGuestId());
                    } else {
                        ps.setNull(4, java.sql.Types.BIGINT);
                    }
                })
                .build();
        
        return new ProgressReportingWriter<>("SpaceContents", jdbcWriter, 10000);
    }

    @Bean
    public ItemWriter<Photo> photoWriter() {
        JdbcBatchItemWriter<Photo> jdbcWriter = new JdbcBatchItemWriterBuilder<Photo>()
                .dataSource(dataSource)
                .sql("INSERT INTO photo (id, original_name, path, captured_at, capacity, created_at) VALUES (?, ?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter((photo, ps) -> {
                    ps.setLong(1, photo.getId());
                    ps.setString(2, photo.getOriginalName());
                    ps.setString(3, photo.getPath());
                    if (photo.getCapturedAt() != null) {
                        ps.setTimestamp(4, java.sql.Timestamp.valueOf(photo.getCapturedAt()));
                    } else {
                        ps.setNull(4, java.sql.Types.TIMESTAMP);
                    }
                    ps.setLong(5, photo.getCapacity());
                    ps.setTimestamp(6, java.sql.Timestamp.valueOf(photo.getCreatedAt()));
                })
                .build();
        
        return new ProgressReportingWriter<>("Photos", jdbcWriter, 10000);
    }

    @Bean
    public Job csvImportJob(JobRepository jobRepository, Step spaceStep, Step hostStep, 
                           Step spaceHostMapStep, Step hostKakaoStep, Step guestStep, 
                           Step spaceContentStep, Step photoStep) {
        return new JobBuilder("csvImportJob", jobRepository)
                .start(spaceStep)
                .next(hostStep)
                .next(spaceHostMapStep)
                .next(hostKakaoStep)
                .next(guestStep)
                .next(spaceContentStep)
                .next(photoStep)
                .build();
    }

    @Bean
    public Job jsonImportJob(JobRepository jobRepository, Step jsonProcessingStep) {
        return new JobBuilder("jsonImportJob", jobRepository)
                .start(jsonProcessingStep)
                .build();
    }

    @Bean
    public JsonSpaceItemReader jsonSpaceItemReader() {
        // 재시작 시 스킵 수를 동적으로 결정하는 로직이 필요하지만
        // 지금은 기본값 0으로 설정
        return new JsonSpaceItemReader(jsonFilePath, 0);
    }

    @Bean
    public Step spaceStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, 
                         ItemWriter<Space> spaceWriter) {
        return new StepBuilder("spaceStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.Space>chunk(1000, transactionManager)
                .reader(spaceBatchStep.reader())
                .processor(spaceBatchStep.processor())
                .writer(spaceWriter)
                .build();
    }

    @Bean
    public Step hostStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                        ItemWriter<Host> hostWriter) {
        return new StepBuilder("hostStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.Host>chunk(1000, transactionManager)
                .reader(hostBatchStep.reader())
                .processor(hostBatchStep.processor())
                .writer(hostWriter)
                .build();
    }

    @Bean
    public Step spaceHostMapStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                               ItemWriter<SpaceHostMap> spaceHostMapWriter) {
        return new StepBuilder("spaceHostMapStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.SpaceHostMap>chunk(1000, transactionManager)
                .reader(spaceHostMapBatchStep.reader())
                .processor(spaceHostMapBatchStep.processor())
                .writer(spaceHostMapWriter)
                .build();
    }

    @Bean
    public Step hostKakaoStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                            ItemWriter<HostKakao> hostKakaoWriter) {
        return new StepBuilder("hostKakaoStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.HostKakao>chunk(1000, transactionManager)
                .reader(hostKakaoBatchStep.reader())
                .processor(hostKakaoBatchStep.processor())
                .writer(hostKakaoWriter)
                .build();
    }

    @Bean
    public Step guestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         ItemWriter<Guest> guestWriter) {
        return new StepBuilder("guestStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.Guest>chunk(1000, transactionManager)
                .reader(guestBatchStep.reader())
                .processor(guestBatchStep.processor())
                .writer(guestWriter)
                .build();
    }

    @Bean
    public Step spaceContentStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                               ItemWriter<SpaceContent> spaceContentWriter) {
        return new StepBuilder("spaceContentStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.SpaceContent>chunk(1000, transactionManager)
                .reader(spaceContentBatchStep.reader())
                .processor(spaceContentBatchStep.processor())
                .writer(spaceContentWriter)
                .build();
    }

    @Bean
    public Step photoStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         ItemWriter<Photo> photoWriter) {
        return new StepBuilder("photoStep", jobRepository)
                .<String[], io.spring.imagegenerator.entity.Photo>chunk(1000, transactionManager)
                .reader(photoBatchStep.reader())
                .processor(photoBatchStep.processor())
                .writer(photoWriter)
                .build();
    }

    @Bean
    public Step jsonProcessingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("jsonProcessingStep", jobRepository)
                .<JsonSpaceData.SpaceData, JsonProcessedData>chunk(100, transactionManager)
                .reader(jsonSpaceItemReader())
                .processor(jsonSpaceProcessor)
                .writer(jsonDataWriter)
                .build();
    }

    // Progress Reporting Wrapper for JdbcBatchItemWriter
    public static class ProgressReportingWriter<T> implements ItemWriter<T> {
        private final String entityName;
        private final ItemWriter<T> delegate;
        private final long reportInterval;
        private long processedCount = 0;
        private StepExecution stepExecution;
        private boolean isRestart = false;
        private long startReadCount = 0;
        private Long firstRecordId = null;
        private Long lastRecordId = null;
        
        public ProgressReportingWriter(String entityName, ItemWriter<T> delegate, long reportInterval) {
            this.entityName = entityName;
            this.delegate = delegate;
            this.reportInterval = reportInterval;
        }
        
        @BeforeStep
        public void beforeStep(StepExecution stepExecution) {
            this.stepExecution = stepExecution;
            this.startReadCount = stepExecution.getReadCount();
            this.isRestart = startReadCount > 0;
            
            if (isRestart) {
                System.out.printf("  %s: Restarting from CSV line %d\n", entityName, startReadCount + 1);
            } else {
                System.out.printf("  %s: Starting from beginning\n", entityName);
            }
        }
        
        @Override
        public void write(Chunk<? extends T> chunk) throws Exception {
            delegate.write(chunk);
            
            processedCount += chunk.size();
            
            // Extract IDs from the chunk to show actual data range
            if (!chunk.isEmpty()) {
                Object firstItem = chunk.getItems().get(0);
                Object lastItem = chunk.getItems().get(chunk.size() - 1);
                
                try {
                    // Try to get ID using reflection
                    Long firstId = (Long) firstItem.getClass().getMethod("getId").invoke(firstItem);
                    Long lastId = (Long) lastItem.getClass().getMethod("getId").invoke(lastItem);
                    
                    if (firstRecordId == null) firstRecordId = firstId;
                    lastRecordId = lastId;
                } catch (Exception e) {
                    // Fallback if reflection fails
                }
            }
            
            // Report progress every reportInterval items
            if (processedCount % reportInterval == 0 || 
                (processedCount % reportInterval < chunk.size() && processedCount >= reportInterval)) {
                long reportCount = (processedCount / reportInterval) * reportInterval;
                if (reportCount > 0) {
                    long totalReadCount = startReadCount + reportCount;
                    
                    if (lastRecordId != null) {
                        System.out.printf("  ✓ %s: ID %d\n", entityName, lastRecordId);
                    } else {
                        System.out.printf("  ✓ %s: %d records\n", entityName, reportCount);
                    }
                }
            }
        }
    }
}