package io.spring.imagegenerator.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CsvDataService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job csvImportJob;

    public void executeDataImport() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            System.out.println("Starting CSV import batch job...");
            jobLauncher.run(csvImportJob, jobParameters);
            System.out.println("CSV import batch job completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to execute CSV import batch job: " + e.getMessage());
            throw new RuntimeException("Batch job execution failed", e);
        }
    }
}
