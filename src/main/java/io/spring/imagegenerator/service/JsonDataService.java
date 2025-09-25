package io.spring.imagegenerator.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class JsonDataService {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    @Qualifier("jsonImportJob")
    private Job jsonImportJob;
    
    @Autowired
    private JobExplorer jobExplorer;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private void applyMySQLOptimizations() {
        try {
            jdbcTemplate.execute("SET sql_log_bin = 0");
            jdbcTemplate.execute("SET autocommit = 0");
            jdbcTemplate.execute("SET unique_checks = 0");
            jdbcTemplate.execute("SET foreign_key_checks = 0");
        } catch (Exception e) {
            System.out.printf("MySQL optimization settings failed: %s%n", e.getMessage());
        }
    }
    
    private void restoreMySQLDefaults() {
        try {
            jdbcTemplate.execute("SET sql_log_bin = 1");
            jdbcTemplate.execute("SET autocommit = 1");
            jdbcTemplate.execute("SET unique_checks = 1");
            jdbcTemplate.execute("SET foreign_key_checks = 1");
        } catch (Exception e) {
            System.out.printf("MySQL default settings restoration failed: %s%n", e.getMessage());
        }
    }
    
    public void executeJsonDataImport() {
        try {
            // Apply MySQL optimizations before processing
            applyMySQLOptimizations();
            // JobParameters jobParameters = new JobParametersBuilder()
            //         .addLong("time", System.currentTimeMillis())
            //         .toJobParameters();
            JobParameters jobParameters = new JobParameters();
            
            // 기존에 실행중이거나 완료되지 않은 Job이 있는지 확인
            List<JobInstance> jobInstances = jobExplorer.getJobInstances(jsonImportJob.getName(), 0, 20);
            
            long previousProcessedCount = 0;
            boolean foundRunning = false;
            boolean foundFailed = false;
            
            // 모든 JobInstance를 검사하여 STARTED 상태를 처리
            for (JobInstance jobInstance : jobInstances) {
                List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                
                for (JobExecution execution : jobExecutions) {
                    System.out.printf("Checking JobExecution ID=%d, Status=%s\n", execution.getId(), execution.getStatus());
                    
                    if (execution.getStatus() == BatchStatus.STARTED || 
                        execution.getStatus() == BatchStatus.STARTING || 
                        execution.getStatus() == BatchStatus.STOPPING) {
                        
                        foundRunning = true;
                        System.out.printf("*** Found RUNNING job execution ID=%d with status: %s ***\n", execution.getId(), execution.getStatus());
                        
                        // 현재 진행 상황 확인
                        for (StepExecution stepExecution : execution.getStepExecutions()) {
                            long stepReadCount = stepExecution.getReadCount();
                            System.out.printf("  Step '%s': Read %d items, Status=%s\n", 
                                stepExecution.getStepName(), stepReadCount, stepExecution.getStatus());
                            previousProcessedCount += stepReadCount;
                        }
                        
                        System.out.printf("*** Forcing JobExecution ID=%d to FAILED status ***\n", execution.getId());
                        
                        // 실행중인 Job과 Step을 강제로 FAILED 상태로 변경
                        execution.setStatus(BatchStatus.FAILED);
                        execution.setEndTime(LocalDateTime.now());
                        execution.setExitStatus(execution.getExitStatus().addExitDescription("Forced to FAILED by restart logic"));
                        
                        // 실행중인 Step들도 FAILED로 변경
                        for (StepExecution stepExecution : execution.getStepExecutions()) {
                            if (stepExecution.getStatus() == BatchStatus.STARTED || 
                                stepExecution.getStatus() == BatchStatus.STARTING ||
                                stepExecution.getStatus() == BatchStatus.STOPPING) {
                                stepExecution.setStatus(BatchStatus.FAILED);
                                stepExecution.setEndTime(LocalDateTime.now());
                                stepExecution.setExitStatus(stepExecution.getExitStatus().addExitDescription("Forced to FAILED by restart logic"));
                                jobRepository.update(stepExecution);
                                System.out.printf("  Step '%s' marked as FAILED\n", stepExecution.getStepName());
                            }
                        }
                        
                        jobRepository.update(execution);
                        System.out.printf("*** JobExecution ID=%d successfully marked as FAILED ***\n", execution.getId());
                        System.out.printf("Previous session processed: %d spaces\n", previousProcessedCount);
                    }
                }
            }
            
            // 다시 한번 전체 검사하여 FAILED 상태 확인
            if (foundRunning) {
                System.out.println("*** Re-checking all JobExecutions after status change ***");
                jobInstances = jobExplorer.getJobInstances(jsonImportJob.getName(), 0, 20);
                
                for (JobInstance jobInstance : jobInstances) {
                    List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                    
                    for (JobExecution execution : jobExecutions) {
                        System.out.printf("Re-check: JobExecution ID=%d, Status=%s\n", execution.getId(), execution.getStatus());
                        
                        if (execution.getStatus() == BatchStatus.STARTED || 
                            execution.getStatus() == BatchStatus.STARTING || 
                            execution.getStatus() == BatchStatus.STOPPING) {
                            System.err.printf("ERROR: JobExecution ID=%d still has status %s after attempted change!\n", execution.getId(), execution.getStatus());
                            throw new RuntimeException("Failed to change JobExecution status from STARTED to FAILED");
                        }
                        
                        if (execution.getStatus() == BatchStatus.FAILED && !foundFailed) {
                            foundFailed = true;
                            System.out.printf("Confirmed: JobExecution ID=%d is now FAILED, ready for restart\n", execution.getId());
                            
                            // 진행량 재계산 (강제 변경된 경우를 위해)
                            if (previousProcessedCount == 0) {
                                for (StepExecution stepExecution : execution.getStepExecutions()) {
                                    previousProcessedCount += stepExecution.getReadCount();
                                    System.out.printf("  Failed step '%s': Read %d items\n", stepExecution.getStepName(), stepExecution.getReadCount());
                                }
                            }
                            System.out.printf("Resuming from space #%d\n", previousProcessedCount + 1);
                        }
                    }
                }
            } else {
                // RUNNING 상태가 아니었다면 FAILED 또는 COMPLETED 처리
                for (JobInstance jobInstance : jobInstances) {
                    List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                    
                    for (JobExecution execution : jobExecutions) {
                        if (execution.getStatus() == BatchStatus.FAILED && !foundFailed) {
                            foundFailed = true;
                            System.out.println("Found failed job execution. Attempting restart...");
                            
                            for (StepExecution stepExecution : execution.getStepExecutions()) {
                                previousProcessedCount += stepExecution.getReadCount();
                                System.out.printf("  Previous failed step '%s': Read %d items\n", stepExecution.getStepName(), stepExecution.getReadCount());
                            }
                            System.out.printf("Resuming from space #%d\n", previousProcessedCount + 1);
                            break;
                        }
                        
                        if (execution.getStatus() == BatchStatus.COMPLETED) {
                            for (StepExecution stepExecution : execution.getStepExecutions()) {
                                previousProcessedCount += stepExecution.getReadCount();
                            }
                            System.out.printf("Previous completed session processed: %d spaces\n", previousProcessedCount);
                        }
                    }
                    
                    if (foundFailed) {
                        break;
                    }
                }
            }
            
            if (!foundRunning && !foundFailed && previousProcessedCount > 0) {
                System.out.printf("Starting new session. Total spaces processed so far: %d\n", previousProcessedCount);
            }

            System.out.println("=== Starting JSON Data Import ===");
            if (previousProcessedCount > 0) {
                System.out.printf("Continuing from space #%d (need to skip %d spaces)\n", previousProcessedCount + 1, previousProcessedCount);
                // TODO: JsonSpaceItemReader에 skipCount 전달 방식 개선 필요
                // 현재는 ID 생성기 초기화로 대체
            }
            
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncher.run(jsonImportJob, jobParameters);
            
            // 이번 세션 처리량 계산
            long currentSessionCount = 0;
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                currentSessionCount += stepExecution.getReadCount();
            }
            
            long totalProcessedCount = previousProcessedCount + currentSessionCount;
            
            System.out.println("JSON import batch job completed with status: " + jobExecution.getExitStatus());
            System.out.printf("This session processed: %d spaces\n", currentSessionCount);
            System.out.printf("Total spaces processed across all sessions: %d\n", totalProcessedCount);
            
            long endTime = System.currentTimeMillis();
            System.out.println("=== JSON Data Import Completed ===");
            System.out.printf("Total execution time: %d ms (%.2f seconds)\n", 
                (endTime - startTime), (endTime - startTime) / 1000.0);
            
        } catch (JobExecutionAlreadyRunningException e) {
            System.err.println("Job is already running. This should have been handled above.");
            throw new RuntimeException("Job execution conflict", e);
        } catch (Exception e) {
            System.err.println("Failed to execute JSON import batch job: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Batch job execution failed", e);
        } finally {
            // Restore MySQL defaults after completion
            restoreMySQLDefaults();
        }
    }
}
