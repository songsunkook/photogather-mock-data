package io.spring.imagegenerator.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class ProgressListener implements StepExecutionListener {

    private long totalItems = 0;
    private long lastReported = 0;
    private static final long REPORT_INTERVAL = 10000;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        totalItems = 0;
        lastReported = 0;
        System.out.printf("  %s: Starting...\n", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long writeCount = stepExecution.getWriteCount();
        String stepName = stepExecution.getStepName().replace("Step", "");
        
        System.out.printf("  %s: Completed %d records (100%%)\n", 
            stepName, writeCount);
        
        return stepExecution.getExitStatus();
    }
    
    public void reportProgress(String stepName, long currentCount) {
        if (currentCount - lastReported >= REPORT_INTERVAL) {
            System.out.printf("  %s: Processed %d records\n", stepName, currentCount);
            lastReported = currentCount;
        }
    }
}