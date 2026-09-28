package com.gem.evidencegraph.service;

import com.gem.evidencegraph.dto.HealthResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Service
public class HealthServiceImpl implements HealthService {

    private static final String APPLICATION_NAME = "GeM EvidenceGraph";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final DataSource dataSource;

    public HealthServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HealthResponseDto getHealthStatus() {
        boolean isDbHealthy = checkDatabaseConnectivity();

        return HealthResponseDto.builder()
                .status(isDbHealthy ? STATUS_UP : STATUS_DOWN)
                .application(APPLICATION_NAME)
                .build();
    }

    private boolean checkDatabaseConnectivity() {
        if (dataSource == null) {
            log.warn("DataSource is null in HealthServiceImpl");
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            if (!connection.isValid(2)) {
                return false;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
            return true;
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

}

