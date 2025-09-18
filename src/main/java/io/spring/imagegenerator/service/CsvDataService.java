package io.spring.imagegenerator.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
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
    private JobExplorer jobExplorer;

    @Autowired
    private JobRepository jobRepository;

    public void executeDataImport() {
        try {
/*
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
*/
            JobParameters jobParameters = new JobParameters();
            
            // 기존에 실행중이거나 완료되지 않은 Job이 있는지 확인
            List<JobInstance> jobInstances = jobExplorer.getJobInstances(csvImportJob.getName(), 0, 1);
            
            if (!jobInstances.isEmpty()) {
                JobInstance jobInstance = jobInstances.get(0);
                List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
                
                for (JobExecution execution : jobExecutions) {
                    if (execution.getStatus() == BatchStatus.STARTED || 
                        execution.getStatus() == BatchStatus.STARTING || 
                        execution.getStatus() == BatchStatus.STOPPING) {
                        
                        System.out.println("Found running job execution: " + execution.getId() + " with status: " + execution.getStatus());
                        System.out.println("Stopping the running job execution...");
                        
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
                        System.out.println("Previous job execution and steps marked as FAILED. Ready for restart.");
                    }
                    
                    if (execution.getStatus() == BatchStatus.FAILED) {
                        System.out.println("Found failed job execution. Attempting restart...");
                        break;
                    }
                }
            }

            System.out.println("Starting CSV import batch job...");
            JobExecution jobExecution = jobLauncher.run(csvImportJob, jobParameters);
            System.out.println("CSV import batch job completed with status: " + jobExecution.getExitStatus());
            
        } catch (JobExecutionAlreadyRunningException e) {
            System.err.println("Job is already running. This should have been handled above.");
            throw new RuntimeException("Job execution conflict", e);
        } catch (Exception e) {
            System.err.println("Failed to execute CSV import batch job: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Batch job execution failed", e);
        }
    }
}
