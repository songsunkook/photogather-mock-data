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
import org.springframework.stereotype.Service;

@Service
public class CsvDataService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job csvImportJob;
    
    @Autowired
    private Job csvCyclicImportJob;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobRepository jobRepository;

    public void executeDataImport() {
        executeCyclicDataImport();
    }
    
    public void executeCyclicDataImport() {
        try {
            // Import CyclicTableStep to access completion methods
            Class<?> cyclicTableStepClass = Class.forName("io.spring.imagegenerator.batch.step.CyclicTableStep");
            java.lang.reflect.Method isAllDataProcessedMethod = cyclicTableStepClass.getMethod("isAllDataProcessed");
            java.lang.reflect.Method printProgressStatusMethod = cyclicTableStepClass.getMethod("printProgressStatus");
            
            int cycleCount = 1;
            
            while (true) {
                System.out.printf("%n=== Cycle %d ===%n", cycleCount);
                
                // Check if all data has been processed before starting a new cycle
                boolean allProcessed = (Boolean) isAllDataProcessedMethod.invoke(null);
                if (allProcessed) {
                    System.out.println("All CSV data processed successfully.");
                    printProgressStatusMethod.invoke(null);
                    break;
                }
                
                long cycleStartTime = System.currentTimeMillis();
                
                // Create unique JobParameters for each cycle to allow restart
                JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("cycleNumber", (long) cycleCount)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
                
                // 기존에 실행중이거나 완료되지 않은 Job이 있는지 확인
                List<JobInstance> jobInstances = jobExplorer.getJobInstances(csvCyclicImportJob.getName(), 0, 1);
                
                if (!jobInstances.isEmpty()) {
                    JobInstance jobInstance = jobInstances.get(0);
                    List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                    
                    for (JobExecution execution : jobExecutions) {
                        if (execution.getStatus() == BatchStatus.STARTED || 
                            execution.getStatus() == BatchStatus.STARTING || 
                            execution.getStatus() == BatchStatus.STOPPING) {
                            
                            // 실행중인 Job과 Step을 강제로 FAILED 상태로 변경
                            execution.setStatus(BatchStatus.FAILED);
                            execution.setEndTime(LocalDateTime.now());
                            
                            // 실행중인 Step들도 FAILED로 변경
                            for (StepExecution stepExecution : execution.getStepExecutions()) {
                                if (stepExecution.getStatus() == BatchStatus.STARTED || 
                                    stepExecution.getStatus() == BatchStatus.STARTING ||
                                    stepExecution.getStatus() == BatchStatus.STOPPING) {
                                    stepExecution.setStatus(BatchStatus.FAILED);
                                    stepExecution.setEndTime(LocalDateTime.now());
                                    jobRepository.update(stepExecution);
                                }
                            }
                            
                            jobRepository.update(execution);
                        }
                    }
                }

                JobExecution jobExecution = jobLauncher.run(csvCyclicImportJob, jobParameters);
                
                long cycleEndTime = System.currentTimeMillis();
                long cycleDuration = cycleEndTime - cycleStartTime;
                
                System.out.printf("Cycle %d completed (%dms)%n", cycleCount, cycleDuration);
                
                // Print progress after each cycle
                printProgressStatusMethod.invoke(null);
                
                // Check completion again after this cycle
                allProcessed = (Boolean) isAllDataProcessedMethod.invoke(null);
                if (allProcessed) {
                    System.out.println("All CSV data processed successfully.");
                    break;
                }
                
                cycleCount++;
                
                // Add a small delay between cycles to avoid overwhelming the system
                Thread.sleep(1000);
            }
            
        } catch (JobExecutionAlreadyRunningException e) {
            throw new RuntimeException("Job execution conflict", e);
        } catch (Exception e) {
            throw new RuntimeException("Batch job execution failed", e);
        }
    }
}
