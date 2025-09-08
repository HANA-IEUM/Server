package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferHistory;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferStatus;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferHistoryRepository;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferScheduleRepository;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.transfer.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoTransferService 단위 테스트")
class AutoTransferServiceImplTest {

    @Mock
    private AutoTransferScheduleRepository scheduleRepository;

    @Mock
    private AutoTransferHistoryRepository historyRepository;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private AutoTransferServiceImpl autoTransferService;

    @Test
    @DisplayName("예정된 자동이체 실행 성공")
    void executeScheduledTransfers_Success() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        
        AutoTransferSchedule schedule1 = createSchedule(1L, 15);
        AutoTransferSchedule schedule2 = createSchedule(2L, 15);
        List<AutoTransferSchedule> schedules = List.of(schedule1, schedule2);

        when(scheduleRepository.findSchedulesForExecution(targetDate, 15))
                .thenReturn(schedules);
        when(historyRepository.findTodayExecution(any(AutoTransferSchedule.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty()); // 오늘 실행된 이력 없음
        when(historyRepository.save(any(AutoTransferHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 이체 성공
        doNothing().when(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());

        // When
        autoTransferService.executeScheduledTransfers(targetDate);

        // Then
        verify(transferService, times(2)).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository, times(2)).save(any(AutoTransferHistory.class));
    }

    @Test
    @DisplayName("이미 실행된 스케줄은 건너뛰기")
    void executeScheduledTransfers_SkipAlreadyExecuted() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        
        AutoTransferSchedule schedule = createSchedule(1L, 15);
        List<AutoTransferSchedule> schedules = List.of(schedule);

        AutoTransferHistory existingHistory = AutoTransferHistory.builder()
                .schedule(schedule)
                .status(AutoTransferStatus.SUCCESS)
                .build();

        when(scheduleRepository.findSchedulesForExecution(targetDate, 15))
                .thenReturn(schedules);
        when(historyRepository.findTodayExecution(any(AutoTransferSchedule.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existingHistory)); // 이미 성공한 이력 존재

        // When
        autoTransferService.executeScheduledTransfers(targetDate);

        // Then
        verify(transferService, never()).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository, never()).save(any(AutoTransferHistory.class));
    }

    @Test
    @DisplayName("단일 자동이체 실행 성공")
    void executeTransfer_Success() {
        // Given
        AutoTransferSchedule schedule = createSchedule(1L, 15);
        
        when(historyRepository.save(any(AutoTransferHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 이체 성공
        doNothing().when(transferService).executeAutoTransfer(
                eq(schedule.getFromAccount().getId()),
                eq(schedule.getToAccount().getId()),
                eq(schedule.getAmount()),
                eq(schedule.getId())
        );

        // When
        AutoTransferHistory result = autoTransferService.executeTransfer(schedule);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSchedule()).isEqualTo(schedule);
        assertThat(result.getStatus()).isEqualTo(AutoTransferStatus.SUCCESS);
        assertThat(result.getFailureReason()).isNull();
        assertThat(result.getRetryCount()).isEqualTo(0);
        
        verify(transferService).executeAutoTransfer(
                eq(schedule.getFromAccount().getId()),
                eq(schedule.getToAccount().getId()),
                eq(schedule.getAmount()),
                eq(schedule.getId())
        );
        verify(historyRepository).save(any(AutoTransferHistory.class));
    }

    @Test
    @DisplayName("단일 자동이체 실행 실패")
    void executeTransfer_Failure() {
        // Given
        AutoTransferSchedule schedule = createSchedule(1L, 15);
        String failureMessage = "잔액이 부족합니다";
        
        when(historyRepository.save(any(AutoTransferHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 이체 실패
        doThrow(new RuntimeException(failureMessage))
                .when(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());

        // When
        AutoTransferHistory result = autoTransferService.executeTransfer(schedule);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSchedule()).isEqualTo(schedule);
        assertThat(result.getStatus()).isEqualTo(AutoTransferStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo(failureMessage);
        assertThat(result.getRetryCount()).isEqualTo(0);
        
        verify(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository).save(any(AutoTransferHistory.class));
    }

    @Test
    @DisplayName("실패한 자동이체 재시도 성공")
    void retryFailedTransfers_Success() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        Integer retryCount = 1;
        
        AutoTransferHistory failedHistory = AutoTransferHistory.builder()
                .id(1L)
                .schedule(createSchedule(1L, 15))
                .fromAccount(createMainAccount())
                .toAccount(createMoneyBoxAccount())
                .amount(BigDecimal.valueOf(100000))
                .status(AutoTransferStatus.FAILED)
                .retryCount(retryCount)
                .build();

        List<AutoTransferHistory> failedTransfers = List.of(failedHistory);
        
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        when(historyRepository.findFailedTransfersForRetry(startOfDay, endOfDay, retryCount))
                .thenReturn(failedTransfers);
        when(historyRepository.save(any(AutoTransferHistory.class)))
                .thenReturn(failedHistory);

        // 재시도 성공
        doNothing().when(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());

        // When
        autoTransferService.retryFailedTransfers(targetDate, retryCount);

        // Then
        verify(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository).save(failedHistory);
        assertThat(failedHistory.getStatus()).isEqualTo(AutoTransferStatus.SUCCESS);
        assertThat(failedHistory.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("실패한 자동이체 재시도 실패 - 재시도 횟수 증가")
    void retryFailedTransfers_RetryFailure() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        Integer retryCount = 1;
        
        AutoTransferHistory failedHistory = AutoTransferHistory.builder()
                .id(1L)
                .schedule(createSchedule(1L, 15))
                .fromAccount(createMainAccount())
                .toAccount(createMoneyBoxAccount())
                .amount(BigDecimal.valueOf(100000))
                .status(AutoTransferStatus.FAILED)
                .retryCount(retryCount)
                .build();

        List<AutoTransferHistory> failedTransfers = List.of(failedHistory);
        
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        String failureMessage = "재시도 실패";

        when(historyRepository.findFailedTransfersForRetry(startOfDay, endOfDay, retryCount))
                .thenReturn(failedTransfers);
        when(historyRepository.save(any(AutoTransferHistory.class)))
                .thenReturn(failedHistory);

        // 재시도 실패
        doThrow(new RuntimeException(failureMessage))
                .when(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());

        // When
        autoTransferService.retryFailedTransfers(targetDate, retryCount);

        // Then
        verify(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository).save(failedHistory);
        assertThat(failedHistory.getRetryCount()).isEqualTo(retryCount + 1);
        assertThat(failedHistory.getStatus()).isEqualTo(AutoTransferStatus.RETRY);
        assertThat(failedHistory.getFailureReason()).isEqualTo(failureMessage);
    }

    @Test
    @DisplayName("최종 재시도 실패 - 상태를 FAILED로 변경")
    void retryFailedTransfers_FinalFailure() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        Integer retryCount = 2; // 마지막 재시도
        
        AutoTransferHistory failedHistory = AutoTransferHistory.builder()
                .id(1L)
                .schedule(createSchedule(1L, 15))
                .fromAccount(createMainAccount())
                .toAccount(createMoneyBoxAccount())
                .amount(BigDecimal.valueOf(100000))
                .status(AutoTransferStatus.RETRY)
                .retryCount(retryCount)
                .build();

        List<AutoTransferHistory> failedTransfers = List.of(failedHistory);
        
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        String failureMessage = "최종 재시도 실패";

        when(historyRepository.findFailedTransfersForRetry(startOfDay, endOfDay, retryCount))
                .thenReturn(failedTransfers);
        when(historyRepository.save(any(AutoTransferHistory.class)))
                .thenReturn(failedHistory);

        // 최종 재시도 실패
        doThrow(new RuntimeException(failureMessage))
                .when(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());

        // When
        autoTransferService.retryFailedTransfers(targetDate, retryCount);

        // Then
        verify(transferService).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository).save(failedHistory);
        assertThat(failedHistory.getRetryCount()).isEqualTo(3); // 최대 재시도 횟수
        assertThat(failedHistory.getStatus()).isEqualTo(AutoTransferStatus.FAILED); // 최종 실패
        assertThat(failedHistory.getFailureReason()).isEqualTo(failureMessage);
    }

    @Test
    @DisplayName("실행 대상 스케줄 조회")
    void getSchedulesToExecute() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        int targetDay = 15;
        
        List<AutoTransferSchedule> schedules = List.of(
                createSchedule(1L, 15),
                createSchedule(2L, 15)
        );

        when(scheduleRepository.findSchedulesForExecution(targetDate, targetDay))
                .thenReturn(schedules);

        // When
        List<AutoTransferSchedule> result = autoTransferService.getSchedulesToExecute(targetDate);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(schedules);
        verify(scheduleRepository).findSchedulesForExecution(targetDate, targetDay);
    }

    @Test
    @DisplayName("재시도 대상이 없는 경우")
    void retryFailedTransfers_NoFailures() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        Integer retryCount = 0;
        
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        when(historyRepository.findFailedTransfersForRetry(startOfDay, endOfDay, retryCount))
                .thenReturn(List.of());

        // When
        autoTransferService.retryFailedTransfers(targetDate, retryCount);

        // Then
        verify(transferService, never()).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository, never()).save(any(AutoTransferHistory.class));
    }

    @Test
    @DisplayName("예정된 자동이체가 없는 경우")
    void executeScheduledTransfers_NoSchedules() {
        // Given
        LocalDate targetDate = LocalDate.of(2024, 3, 15);

        when(scheduleRepository.findSchedulesForExecution(targetDate, 15))
                .thenReturn(List.of());

        // When
        autoTransferService.executeScheduledTransfers(targetDate);

        // Then
        verify(transferService, never()).executeAutoTransfer(anyLong(), anyLong(), any(BigDecimal.class), anyLong());
        verify(historyRepository, never()).save(any(AutoTransferHistory.class));
    }

    private Member createMember() {
        return Member.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .name("테스트사용자")
                .gender(Gender.M)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }

    private Account createMainAccount() {
        return Account.builder()
                .id(1L)
                .member(createMember())
                .number("12345678901234")
                .name("주계좌")
                .accountType(AccountType.MAIN)
                .balance(BigDecimal.valueOf(1000000))
                .deleted(false)
                .build();
    }

    private Account createMoneyBoxAccount() {
        return Account.builder()
                .id(2L)
                .member(createMember())
                .number("98765432109")
                .name("머니박스")
                .accountType(AccountType.MONEY_BOX)
                .balance(BigDecimal.valueOf(100000))
                .deleted(false)
                .build();
    }

    private AutoTransferSchedule createSchedule(Long id, Integer transferDay) {
        return AutoTransferSchedule.builder()
                .id(id)
                .fromAccount(createMainAccount())
                .toAccount(createMoneyBoxAccount())
                .amount(BigDecimal.valueOf(100000))
                .transferDay(transferDay)
                .validFrom(LocalDate.of(2024, 3, 1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();
    }
}