package com.hanaieum.server.domain.autoTransfer.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferHistory;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferStatus;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("AutoTransferHistoryRepository 테스트")
class AutoTransferHistoryRepositoryTest {

    @Autowired
    private AutoTransferHistoryRepository autoTransferHistoryRepository;

    @Autowired
    private AutoTransferScheduleRepository autoTransferScheduleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("재시도 대상 실패한 이체 조회 - 특정 재시도 횟수")
    void findFailedTransfersForRetry() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // 재시도 횟수가 0인 실패한 이체
        AutoTransferHistory failedHistory = createAndSaveHistory(
            schedule, fromAccount, toAccount, today, AutoTransferStatus.FAILED, 0
        );
        
        // 재시도 횟수가 1인 실패한 이체 (다른 재시도 횟수)
        createAndSaveHistory(
            schedule, fromAccount, toAccount, today, AutoTransferStatus.FAILED, 1
        );
        
        // 성공한 이체 (제외되어야 함)
        createAndSaveHistory(
            schedule, fromAccount, toAccount, today, AutoTransferStatus.SUCCESS, 0
        );

        // When
        List<AutoTransferHistory> result = autoTransferHistoryRepository
                .findFailedTransfersForRetry(startOfDay, endOfDay, 0);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(failedHistory.getId());
        assertThat(result.get(0).getStatus()).isEqualTo(AutoTransferStatus.FAILED);
        assertThat(result.get(0).getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("재시도 대상 조회 - RETRY 상태도 포함")
    void findFailedTransfersForRetry_IncludeRetryStatus() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // FAILED 상태
        AutoTransferHistory failedHistory = createAndSaveHistory(
            schedule, fromAccount, toAccount, today, AutoTransferStatus.FAILED, 1
        );
        
        // RETRY 상태
        AutoTransferHistory retryHistory = createAndSaveHistory(
            schedule, fromAccount, toAccount, today, AutoTransferStatus.RETRY, 1
        );

        // When
        List<AutoTransferHistory> result = autoTransferHistoryRepository
                .findFailedTransfersForRetry(startOfDay, endOfDay, 1);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AutoTransferHistory::getId)
                .containsExactlyInAnyOrder(failedHistory.getId(), retryHistory.getId());
    }

    @Test
    @DisplayName("다른 날짜의 실패한 이체는 조회되지 않음")
    void findFailedTransfersForRetry_DifferentDate() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // 어제 실패한 이체 (조회되지 않아야 함)
        createAndSaveHistory(
            schedule, fromAccount, toAccount, yesterday, AutoTransferStatus.FAILED, 0
        );

        // When
        List<AutoTransferHistory> result = autoTransferHistoryRepository
                .findFailedTransfersForRetry(startOfDay, endOfDay, 0);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 스케줄의 오늘 실행 이력 존재 여부 확인")
    void findTodayExecution() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // 오늘 실행된 이력 생성
        AutoTransferHistory todayHistory = createAndSaveHistory(
            schedule, fromAccount, toAccount, today, AutoTransferStatus.SUCCESS, 0
        );

        // When
        Optional<AutoTransferHistory> result = autoTransferHistoryRepository
                .findTodayExecution(schedule, startOfDay, endOfDay);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(todayHistory.getId());
    }

    @Test
    @DisplayName("다른 스케줄의 실행 이력은 조회되지 않음")
    void findTodayExecution_DifferentSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule1 = createAndSaveSchedule(fromAccount, toAccount);
        AutoTransferSchedule schedule2 = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // schedule2에 대한 실행 이력 (schedule1과 다름)
        createAndSaveHistory(
            schedule2, fromAccount, toAccount, today, AutoTransferStatus.SUCCESS, 0
        );

        // When
        Optional<AutoTransferHistory> result = autoTransferHistoryRepository
                .findTodayExecution(schedule1, startOfDay, endOfDay);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("어제 실행된 이력은 오늘 실행 이력으로 조회되지 않음")
    void findTodayExecution_Yesterday() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        // 어제 실행된 이력
        createAndSaveHistory(
            schedule, fromAccount, toAccount, yesterday, AutoTransferStatus.SUCCESS, 0
        );

        // When
        Optional<AutoTransferHistory> result = autoTransferHistoryRepository
                .findTodayExecution(schedule, startOfDay, endOfDay);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("JOIN FETCH를 통한 연관 엔티티 즉시 로딩 검증")
    void findFailedTransfersForRetry_JoinFetch() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount);
        
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime startOfDay = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        createAndSaveHistory(schedule, fromAccount, toAccount, today, AutoTransferStatus.FAILED, 0);

        // When
        List<AutoTransferHistory> result = autoTransferHistoryRepository
                .findFailedTransfersForRetry(startOfDay, endOfDay, 0);

        // Then
        assertThat(result).hasSize(1);
        AutoTransferHistory history = result.get(0);
        
        // JOIN FETCH로 로딩된 연관 엔티티에 접근 가능해야 함 (LazyInitializationException 발생하지 않음)
        assertThat(history.getFromAccount().getId()).isEqualTo(fromAccount.getId());
        assertThat(history.getToAccount().getId()).isEqualTo(toAccount.getId());
    }

    private Member createAndSaveMember() {
        Member member = Member.builder()
                .phoneNumber("01012345678")
                .password("123456")
                .name("테스트사용자")
                .gender(Gender.M)
                .birthDate(LocalDate.of(1990, 1, 1))
                .monthlyLivingCost(2000000)
                .build();
        return memberRepository.save(member);
    }

    private Account createAndSaveAccount(Member member, String accountNumber, AccountType accountType) {
        Account account = Account.builder()
                .member(member)
                .number(accountNumber)
                .name("테스트계좌")
                .bankName("하나은행")
                .balance(BigDecimal.valueOf(1000000))
                .accountType(accountType)
                .password("1234")
                .deleted(false)
                .build();
        return accountRepository.save(account);
    }

    private AutoTransferSchedule createAndSaveSchedule(Account fromAccount, Account toAccount) {
        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(LocalDate.now())
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();
        return autoTransferScheduleRepository.save(schedule);
    }

    private AutoTransferHistory createAndSaveHistory(AutoTransferSchedule schedule, Account fromAccount, 
                                                    Account toAccount, LocalDateTime executedAt, 
                                                    AutoTransferStatus status, Integer retryCount) {
        AutoTransferHistory history = AutoTransferHistory.builder()
                .schedule(schedule)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .executedAt(executedAt)
                .status(status)
                .failureReason(status == AutoTransferStatus.SUCCESS ? null : "테스트 실패 사유")
                .retryCount(retryCount)
                .build();
        return autoTransferHistoryRepository.save(history);
    }
}