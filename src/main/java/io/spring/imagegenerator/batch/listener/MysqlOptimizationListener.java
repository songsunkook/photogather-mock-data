package io.spring.imagegenerator.batch.listener;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class MysqlOptimizationListener implements JobExecutionListener {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("=== Applying MySQL optimizations ===");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.createStatement().execute("SET unique_checks = 0");
            connection.createStatement().execute("SET foreign_key_checks = 0");
            connection.createStatement().execute("SET autocommit = 0");
            connection.commit();
            System.out.println("MySQL optimizations applied successfully");
        } catch (SQLException e) {
            System.err.println("Failed to apply MySQL optimizations: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("=== Restoring MySQL settings ===");
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SET foreign_key_checks = 1");
            connection.createStatement().execute("SET unique_checks = 1");
            connection.createStatement().execute("SET autocommit = 1");
            System.out.println("MySQL settings restored successfully");
        } catch (SQLException e) {
            System.err.println("Failed to restore MySQL settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
