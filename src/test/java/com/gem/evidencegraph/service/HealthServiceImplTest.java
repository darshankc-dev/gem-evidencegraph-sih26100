package com.gem.evidencegraph.service;

import com.gem.evidencegraph.dto.HealthResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Test
    @DisplayName("HealthService should return UP status when database is reachable")
    void shouldReturnHealthStatusUpWhenDatabaseIsReachable() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        HealthService healthService = new HealthServiceImpl(dataSource);
        HealthResponseDto response = healthService.getHealthStatus();

        assertNotNull(response);
        assertEquals("UP", response.getStatus());
        assertEquals("GeM EvidenceGraph", response.getApplication());

        verify(statement).execute("SELECT 1");
    }

    @Test
    @DisplayName("HealthService should return DOWN status when database throws SQLException")
    void shouldReturnHealthStatusDownWhenDatabaseThrowsSQLException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        HealthService healthService = new HealthServiceImpl(dataSource);
        HealthResponseDto response = healthService.getHealthStatus();

        assertNotNull(response);
        assertEquals("DOWN", response.getStatus());
        assertEquals("GeM EvidenceGraph", response.getApplication());
    }

    @Test
    @DisplayName("HealthService should return DOWN status when connection isValid is false")
    void shouldReturnHealthStatusDownWhenConnectionIsInvalid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);
        when(connection.isValid(anyInt())).thenReturn(false);

        HealthService healthService = new HealthServiceImpl(dataSource);
        HealthResponseDto response = healthService.getHealthStatus();

        assertNotNull(response);
        assertEquals("DOWN", response.getStatus());
        assertEquals("GeM EvidenceGraph", response.getApplication());
    }

    @Test
    @DisplayName("HealthService should return DOWN status when DataSource is null")
    void shouldReturnHealthStatusDownWhenDataSourceIsNull() {
        HealthService healthService = new HealthServiceImpl(null);
        HealthResponseDto response = healthService.getHealthStatus();

        assertNotNull(response);
        assertEquals("DOWN", response.getStatus());
        assertEquals("GeM EvidenceGraph", response.getApplication());
    }

}
