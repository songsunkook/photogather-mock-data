package io.spring.imagegenerator.batch.writer;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class OptimizedJdbcBatchItemWriter<T> implements ItemWriter<T> {

    private final JdbcBatchItemWriter<T> delegate;
    private final DataSource dataSource;
    private boolean optimizationApplied = false;

    public OptimizedJdbcBatchItemWriter(JdbcBatchItemWriter<T> delegate, DataSource dataSource) {
        this.delegate = delegate;
        this.dataSource = dataSource;
    }

    @Override
    public void write(Chunk<? extends T> chunk) throws Exception {
        Connection connection = null;
        boolean wasOptimized = false;
        
        try {
            // Spring이 관리하는 트랜잭션 Connection 가져오기
            connection = DataSourceUtils.getConnection(dataSource);
            
            // 아직 최적화가 적용되지 않았으면 적용
            if (!optimizationApplied) {
                System.out.println("[MySQL Optimization] Applying optimizations to current transaction connection");
                connection.createStatement().execute("SET SESSION unique_checks = 0");
                connection.createStatement().execute("SET SESSION foreign_key_checks = 0");
                optimizationApplied = true;
                wasOptimized = true;
                System.out.println("[MySQL Optimization] Optimizations applied successfully");
            }
            
            // 실제 배치 작업 수행
            delegate.write(chunk);
            
        } catch (DataAccessException | SQLException e) {
            System.err.println("[MySQL Optimization] Error during optimized batch write: " + e.getMessage());
            throw e;
        } finally {
            // Connection을 반환 (Spring이 관리)
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }
    
    public void restoreSettings() {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("[MySQL Optimization] Restoring MySQL settings");
            connection.createStatement().execute("SET SESSION foreign_key_checks = 1");
            connection.createStatement().execute("SET SESSION unique_checks = 1");
            System.out.println("[MySQL Optimization] Settings restored successfully");
        } catch (SQLException e) {
            System.err.println("[MySQL Optimization] Failed to restore settings: " + e.getMessage());
        }
    }
}
