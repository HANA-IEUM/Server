package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.autoTransfer.dto.TransferStatus;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferScheduleRepository;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoTransferScheduleService 단위 테스트")
class AutoTransferScheduleServiceImplTest {

    @Mock
    private AutoTransferScheduleRepository autoTransferScheduleRepository;

    @InjectMocks
    private AutoTransferScheduleServiceImpl autoTransferScheduleService;

    @Test
    @DisplayName("자동이체 스케줄 생성 성공 - 다음달부터 시작")
    void createSchedule_Success() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();
        BigDecimal amount = BigDecimal.valueOf(100000);
        Integer transferDay = 15;

        AutoTransferSchedule savedSchedule = AutoTransferSchedule.builder()
                .id(1L)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .transferDay(transferDay)
                .validFrom(LocalDate.now().withDayOfMonth(1).plusMonths(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.save(any(AutoTransferSchedule.class)))
                .thenReturn(savedSchedule);

        // When
        AutoTransferSchedule result = autoTransferScheduleService.createSchedule(
                fromAccount, toAccount, amount, transferDay);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getTransferDay()).isEqualTo(transferDay);
        assertThat(result.getValidFrom()).isEqualTo(LocalDate.now().withDayOfMonth(1).plusMonths(1));
        assertThat(result.getValidTo()).isNull();
        assertThat(result.getActive()).isTrue();

        verify(autoTransferScheduleRepository).save(any(AutoTransferSchedule.class));
    }

    @Test
    @DisplayName("자동이체 활성화 - 변경사항이 있는 경우")
    void updateSchedule_EnableWithChanges() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();
        BigDecimal newAmount = BigDecimal.valueOf(200000);
        Integer newTransferDay = 20;

        // 현재 스케줄 (기존 설정과 다름)
        AutoTransferSchedule currentSchedule = AutoTransferSchedule.builder()
                .id(1L)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now().withDayOfMonth(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(currentSchedule));
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(autoTransferScheduleRepository.save(any(AutoTransferSchedule.class)))
                .thenReturn(currentSchedule);

        // When
        autoTransferScheduleService.updateSchedule(fromAccount, toAccount, true, newAmount, newTransferDay);

        // Then
        verify(autoTransferScheduleRepository, times(2)).save(any(AutoTransferSchedule.class));
        // 1. 새 스케줄 생성, 2. 기존 스케줄 종료 처리
    }

    @Test
    @DisplayName("자동이체 활성화 - 변경사항이 없는 경우 DB 작업 생략")
    void updateSchedule_EnableWithoutChanges() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();
        BigDecimal amount = BigDecimal.valueOf(100000);
        Integer transferDay = 15;

        // 현재 스케줄 (기존 설정과 동일)
        AutoTransferSchedule currentSchedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount) // 동일한 금액
                .transferDay(transferDay) // 동일한 이체일
                .validFrom(LocalDate.now().withDayOfMonth(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        // 미래 스케줄이 이미 존재
        AutoTransferSchedule futureSchedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .transferDay(transferDay)
                .validFrom(LocalDate.now().withDayOfMonth(1).plusMonths(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(currentSchedule));
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(futureSchedule));

        // When
        autoTransferScheduleService.updateSchedule(fromAccount, toAccount, true, amount, transferDay);

        // Then
        verify(autoTransferScheduleRepository, never()).save(any(AutoTransferSchedule.class));
        // 변경사항이 없고 미래 스케줄이 있으므로 DB 작업 생략
    }

    @Test
    @DisplayName("자동이체 비활성화 - 현재 스케줄 종료 및 미래 스케줄 삭제")
    void disableSchedule_Success() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();

        AutoTransferSchedule currentSchedule = AutoTransferSchedule.builder()
                .id(1L)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now().withDayOfMonth(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        AutoTransferSchedule futureSchedule = AutoTransferSchedule.builder()
                .id(2L)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now().withDayOfMonth(1).plusMonths(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(currentSchedule));
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(futureSchedule));
        when(autoTransferScheduleRepository.save(any(AutoTransferSchedule.class)))
                .thenReturn(currentSchedule);

        // When
        autoTransferScheduleService.disableSchedule(fromAccount, toAccount);

        // Then
        verify(autoTransferScheduleRepository, times(2)).save(any(AutoTransferSchedule.class));
        // 1. 현재 스케줄 종료 처리, 2. 미래 스케줄 삭제
        assertThat(currentSchedule.getValidTo()).isNotNull();
        assertThat(futureSchedule.getDeleted()).isTrue();
    }

    @Test
    @DisplayName("자동이체 비활성화 - 스케줄이 없는 경우")
    void updateSchedule_DisableWithoutSchedules() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        autoTransferScheduleService.updateSchedule(fromAccount, toAccount, false, null, null);

        // Then
        verify(autoTransferScheduleRepository, never()).save(any(AutoTransferSchedule.class));
    }

    @Test
    @DisplayName("현재 유효한 스케줄 조회")
    void getCurrentSchedule_Success() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();
        LocalDate today = LocalDate.now();

        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(today.minusDays(10))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(fromAccount, toAccount, today))
                .thenReturn(Optional.of(schedule));

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleService.getCurrentSchedule(fromAccount, toAccount);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(schedule);
        verify(autoTransferScheduleRepository).findActiveSchedule(fromAccount, toAccount, today);
    }

    @Test
    @DisplayName("미래 스케줄 조회")
    void getFutureSchedule_Success() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();
        LocalDate today = LocalDate.now();

        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(today.plusDays(10))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findFutureSchedule(fromAccount, toAccount, today))
                .thenReturn(Optional.of(schedule));

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleService.getFutureSchedule(fromAccount, toAccount);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(schedule);
        verify(autoTransferScheduleRepository).findFutureSchedule(fromAccount, toAccount, today);
    }

    @Test
    @DisplayName("자동이체 상태 조회 - 현재 활성화, 다음달 계속 유지")
    void getTransferStatus_CurrentActiveNextContinue() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();

        AutoTransferSchedule currentSchedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now().withDayOfMonth(1))
                .validTo(null) // validTo가 null이므로 다음달에도 계속 유효
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(currentSchedule));
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        TransferStatus result = autoTransferScheduleService.getTransferStatus(fromAccount, toAccount);

        // Then
        assertThat(result.getCurrentEnabled()).isTrue();
        assertThat(result.getCurrentAmount()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(result.getCurrentTransferDay()).isEqualTo(15);
        assertThat(result.getNextEnabled()).isTrue(); // 다음달에도 계속 유효
        assertThat(result.getNextAmount()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(result.getNextTransferDay()).isEqualTo(15);
    }

    @Test
    @DisplayName("자동이체 상태 조회 - 현재 활성화, 다음달부터 비활성화")
    void getTransferStatus_CurrentActiveNextDisabled() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();

        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        AutoTransferSchedule currentSchedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now().withDayOfMonth(1))
                .validTo(endOfMonth) // 이번달 말까지만 유효
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(currentSchedule));
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        TransferStatus result = autoTransferScheduleService.getTransferStatus(fromAccount, toAccount);

        // Then
        assertThat(result.getCurrentEnabled()).isTrue();
        assertThat(result.getCurrentAmount()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(result.getCurrentTransferDay()).isEqualTo(15);
        assertThat(result.getNextEnabled()).isFalse(); // 다음달부터 비활성화
        assertThat(result.getNextAmount()).isNull();
        assertThat(result.getNextTransferDay()).isNull();
    }

    @Test
    @DisplayName("자동이체 상태 조회 - 미래 스케줄 우선 적용")
    void getTransferStatus_FutureSchedulePriority() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();

        AutoTransferSchedule currentSchedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now().withDayOfMonth(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        AutoTransferSchedule futureSchedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(200000)) // 다른 금액
                .transferDay(20) // 다른 이체일
                .validFrom(LocalDate.now().withDayOfMonth(1).plusMonths(1))
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(currentSchedule));
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.of(futureSchedule));

        // When
        TransferStatus result = autoTransferScheduleService.getTransferStatus(fromAccount, toAccount);

        // Then
        assertThat(result.getCurrentEnabled()).isTrue();
        assertThat(result.getCurrentAmount()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(result.getCurrentTransferDay()).isEqualTo(15);
        
        // 미래 스케줄이 우선 적용됨
        assertThat(result.getNextEnabled()).isTrue();
        assertThat(result.getNextAmount()).isEqualTo(BigDecimal.valueOf(200000));
        assertThat(result.getNextTransferDay()).isEqualTo(20);
    }

    @Test
    @DisplayName("자동이체 상태 조회 - 모든 스케줄이 없는 경우")
    void getTransferStatus_AllDisabled() {
        // Given
        Account fromAccount = createMainAccount();
        Account toAccount = createMoneyBoxAccount();

        when(autoTransferScheduleRepository.findActiveSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(autoTransferScheduleRepository.findFutureSchedule(eq(fromAccount), eq(toAccount), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        TransferStatus result = autoTransferScheduleService.getTransferStatus(fromAccount, toAccount);

        // Then
        assertThat(result.getCurrentEnabled()).isFalse();
        assertThat(result.getCurrentAmount()).isNull();
        assertThat(result.getCurrentTransferDay()).isNull();
        assertThat(result.getNextEnabled()).isFalse();
        assertThat(result.getNextAmount()).isNull();
        assertThat(result.getNextTransferDay()).isNull();
    }

    @Test
    @DisplayName("머니박스 삭제 시 모든 자동이체 스케줄 삭제")
    void deleteAllSchedulesForMoneyBox_Success() {
        // Given
        Account fromAccount = createMainAccount();
        Account moneyBoxAccount = createMoneyBoxAccount();
        
        AutoTransferSchedule schedule1 = AutoTransferSchedule.builder()
                .id(1L)
                .fromAccount(fromAccount)
                .toAccount(moneyBoxAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .active(true)
                .deleted(false)
                .build();
        
        AutoTransferSchedule schedule2 = AutoTransferSchedule.builder()
                .id(2L)
                .fromAccount(fromAccount)
                .toAccount(moneyBoxAccount)
                .amount(BigDecimal.valueOf(200000))
                .transferDay(20)
                .active(true)
                .deleted(false)
                .build();

        List<AutoTransferSchedule> schedules = List.of(schedule1, schedule2);

        when(autoTransferScheduleRepository.findAllByToAccountAndDeletedFalse(moneyBoxAccount))
                .thenReturn(schedules);
        when(autoTransferScheduleRepository.saveAll(anyList()))
                .thenReturn(schedules);

        // When
        autoTransferScheduleService.deleteAllSchedulesForMoneyBox(moneyBoxAccount);

        // Then
        verify(autoTransferScheduleRepository).findAllByToAccountAndDeletedFalse(moneyBoxAccount);
        verify(autoTransferScheduleRepository).saveAll(schedules);
        
        assertThat(schedule1.getDeleted()).isTrue();
        assertThat(schedule1.getActive()).isFalse();
        assertThat(schedule2.getDeleted()).isTrue();
        assertThat(schedule2.getActive()).isFalse();
    }

    @Test
    @DisplayName("머니박스 삭제 - 삭제할 스케줄이 없는 경우")
    void deleteAllSchedulesForMoneyBox_NoSchedules() {
        // Given
        Account moneyBoxAccount = createMoneyBoxAccount();
        
        when(autoTransferScheduleRepository.findAllByToAccountAndDeletedFalse(moneyBoxAccount))
                .thenReturn(List.of());

        // When
        autoTransferScheduleService.deleteAllSchedulesForMoneyBox(moneyBoxAccount);

        // Then
        verify(autoTransferScheduleRepository).findAllByToAccountAndDeletedFalse(moneyBoxAccount);
        verify(autoTransferScheduleRepository, never()).saveAll(anyList());
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
}