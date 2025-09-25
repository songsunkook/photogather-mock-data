package io.spring.imagegenerator.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MysqlOptimizationTest {
    
    @Autowired
    private DataSource dataSource;
    
    public void testOptimizationSettings() {
        System.out.println("=== Testing MySQL Optimization Settings ===");
        
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Connection class: " + connection.getClass().getName());
            
            // 기본 설정 확인
            checkSettings(connection, "BEFORE optimization");
            
            // 최적화 적용
            connection.createStatement().execute("SET SESSION unique_checks = 0");
            connection.createStatement().execute("SET SESSION foreign_key_checks = 0");
            connection.createStatement().execute("SET SESSION autocommit = 0");
            
            // 최적화 후 설정 확인
            checkSettings(connection, "AFTER optimization");
            
        } catch (SQLException e) {
            System.err.println("Error testing MySQL optimization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkSettings(Connection connection, String phase) throws SQLException {
        System.out.println("--- " + phase + " ---");
        
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT @@unique_checks, @@foreign_key_checks, @@autocommit")) {
            if (rs.next()) {
                System.out.printf("unique_checks: %d, foreign_key_checks: %d, autocommit: %d\n", 
                    rs.getInt(1), rs.getInt(2), rs.getInt(3));
            }
        }
    }
}
