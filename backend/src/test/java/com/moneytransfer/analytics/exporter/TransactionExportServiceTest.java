package com.moneytransfer.analytics.exporter;

import com.moneytransfer.analytics.client.SnowflakeClient;
import com.moneytransfer.analytics.model.AnalyticsExportState;
import com.moneytransfer.analytics.repository.AnalyticsExportStateRepository;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionExportService.
 * 
 * Tests verify:
 * - Watermark is loaded correctly
 * - New transactions are queried and exported
 * - Watermark is updated only after successful export
 * - No export occurs when no new records exist
 * - Export is idempotent and repeatable
 */
@ExtendWith(MockitoExtension.class)
class TransactionExportServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AnalyticsExportStateRepository exportStateRepository;

    @Mock
    private SnowflakeClient snowflakeClient;

    @InjectMocks
    private TransactionExportService exportService;

    private LocalDateTime testTime;
    private Instant testInstant;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.of(2026, 2, 12, 10, 0, 0);
        testInstant = testTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    @DisplayName("✅ Export: Exports new transactions and updates watermark")
    void testExportNewTransactionsSuccess() throws SQLException {
        // Arrange
        AnalyticsExportState state = AnalyticsExportState.builder()
                .id(1)
                .lastExportedTimestamp(testInstant)
                .build();

        TransactionLog log1 = createTransactionLog("tx-1", testTime.plusHours(1));
        TransactionLog log2 = createTransactionLog("tx-2", testTime.plusHours(2));
        List<TransactionLog> newTransactions = Arrays.asList(log1, log2);

        when(exportStateRepository.findById(1)).thenReturn(Optional.of(state));
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(newTransactions);
        when(snowflakeClient.uploadAndCopy(anyString(), anyString())).thenReturn(2);

        // Act
        exportService.exportNewTransactions();

        // Assert
        verify(transactionLogRepository).findByCreatedAtGreaterThanOrderByCreatedAtAsc(testTime);
        verify(snowflakeClient).uploadAndCopy(anyString(), anyString());
        
        ArgumentCaptor<AnalyticsExportState> stateCaptor = ArgumentCaptor.forClass(AnalyticsExportState.class);
        verify(exportStateRepository).save(stateCaptor.capture());
        
        AnalyticsExportState savedState = stateCaptor.getValue();
        assertThat(savedState.getLastExportedTimestamp())
                .isEqualTo(testTime.plusHours(2).atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("✅ Export: No export when no new records exist")
    void testNoExportWhenNoNewRecords() throws SQLException {
        // Arrange
        AnalyticsExportState state = AnalyticsExportState.builder()
                .id(1)
                .lastExportedTimestamp(testInstant)
                .build();

        when(exportStateRepository.findById(1)).thenReturn(Optional.of(state));
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        exportService.exportNewTransactions();

        // Assert
        verify(transactionLogRepository).findByCreatedAtGreaterThanOrderByCreatedAtAsc(testTime);
        verify(snowflakeClient, never()).uploadAndCopy(anyString(), anyString());
        verify(exportStateRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Export: Uses EPOCH watermark when no state exists")
    void testUsesEpochWhenNoStateExists() throws SQLException {
        // Arrange
        when(exportStateRepository.findById(1)).thenReturn(Optional.empty());
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        exportService.exportNewTransactions();

        // Assert
        LocalDateTime epochDateTime = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        verify(transactionLogRepository).findByCreatedAtGreaterThanOrderByCreatedAtAsc(epochDateTime);
    }

    @Test
    @DisplayName("✅ Export: Watermark not updated when Snowflake upload fails")
    void testWatermarkNotUpdatedOnFailure() throws SQLException {
        // Arrange
        AnalyticsExportState state = AnalyticsExportState.builder()
                .id(1)
                .lastExportedTimestamp(testInstant)
                .build();

        TransactionLog log = createTransactionLog("tx-1", testTime.plusHours(1));
        List<TransactionLog> newTransactions = Collections.singletonList(log);

        when(exportStateRepository.findById(1)).thenReturn(Optional.of(state));
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(newTransactions);
        when(snowflakeClient.uploadAndCopy(anyString(), anyString()))
                .thenThrow(new SQLException("Connection failed"));

        // Act
        exportService.exportNewTransactions();

        // Assert
        verify(snowflakeClient).uploadAndCopy(anyString(), anyString());
        verify(exportStateRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Export: Repeated execution does not duplicate export")
    void testRepeatedExecutionDoesNotDuplicate() throws SQLException {
        // Arrange
        AnalyticsExportState state = AnalyticsExportState.builder()
                .id(1)
                .lastExportedTimestamp(testInstant)
                .build();

        TransactionLog log1 = createTransactionLog("tx-1", testTime.plusHours(1));
        List<TransactionLog> firstBatch = Collections.singletonList(log1);

        when(exportStateRepository.findById(1))
                .thenReturn(Optional.of(state))
                .thenReturn(Optional.of(state));
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(firstBatch)
                .thenReturn(Collections.emptyList());
        when(snowflakeClient.uploadAndCopy(anyString(), anyString())).thenReturn(1);

        // Act - First execution
        exportService.exportNewTransactions();

        // Update state for second execution
        state.setLastExportedTimestamp(testTime.plusHours(1).atZone(ZoneId.systemDefault()).toInstant());

        // Act - Second execution
        exportService.exportNewTransactions();

        // Assert
        verify(snowflakeClient, times(1)).uploadAndCopy(anyString(), anyString());
    }

    @Test
    @DisplayName("✅ Export: Generates correct CSV format")
    void testGeneratesCorrectCsvFormat() throws SQLException {
        // Arrange
        AnalyticsExportState state = AnalyticsExportState.builder()
                .id(1)
                .lastExportedTimestamp(testInstant)
                .build();

        TransactionLog log = TransactionLog.builder()
                .id("test-id")
                .fromAccountId(100L)
                .toAccountId(200L)
                .idempotencyKey("idempotency-key")
                .transactionType("TRANSFER")
                .amount(new BigDecimal("50.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("50.00"))
                .status("COMPLETED")
                .description("Test transfer")
                .createdAt(testTime.plusHours(1))
                .build();

        when(exportStateRepository.findById(1)).thenReturn(Optional.of(state));
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(Collections.singletonList(log));
        when(snowflakeClient.uploadAndCopy(anyString(), anyString())).thenReturn(1);

        // Act
        exportService.exportNewTransactions();

        // Assert
        ArgumentCaptor<String> csvCaptor = ArgumentCaptor.forClass(String.class);
        verify(snowflakeClient).uploadAndCopy(anyString(), csvCaptor.capture());

        String csv = csvCaptor.getValue();
        assertThat(csv).contains("id,from_account_id,to_account_id");
        assertThat(csv).contains("test-id,100,200");
        assertThat(csv).contains("50.00");
        assertThat(csv).contains("COMPLETED");
    }

    @Test
    @DisplayName("✅ Export: Handles null toAccountId")
    void testHandlesNullToAccountId() throws SQLException {
        // Arrange
        AnalyticsExportState state = AnalyticsExportState.builder()
                .id(1)
                .lastExportedTimestamp(testInstant)
                .build();

        TransactionLog log = TransactionLog.builder()
                .id("test-id")
                .fromAccountId(100L)
                .toAccountId(null)  // Null counterparty
                .idempotencyKey("idempotency-key")
                .transactionType("DEBIT")
                .amount(new BigDecimal("50.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("50.00"))
                .status("COMPLETED")
                .description("Withdrawal")
                .createdAt(testTime.plusHours(1))
                .build();

        when(exportStateRepository.findById(1)).thenReturn(Optional.of(state));
        when(transactionLogRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(any()))
                .thenReturn(Collections.singletonList(log));
        when(snowflakeClient.uploadAndCopy(anyString(), anyString())).thenReturn(1);

        // Act
        exportService.exportNewTransactions();

        // Assert
        ArgumentCaptor<String> csvCaptor = ArgumentCaptor.forClass(String.class);
        verify(snowflakeClient).uploadAndCopy(anyString(), csvCaptor.capture());

        String csv = csvCaptor.getValue();
        // Should have empty field for toAccountId
        assertThat(csv).contains("test-id,100,");
    }

    /**
     * Helper method to create a test TransactionLog.
     */
    private TransactionLog createTransactionLog(String id, LocalDateTime createdAt) {
        return TransactionLog.builder()
                .id(id)
                .fromAccountId(100L)
                .toAccountId(200L)
                .idempotencyKey("key-" + id)
                .transactionType("TRANSFER")
                .amount(new BigDecimal("50.00"))
                .balanceBefore(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("50.00"))
                .status("COMPLETED")
                .description("Test transaction")
                .createdAt(createdAt)
                .build();
    }
}
