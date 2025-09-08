package com.hanaieum.server.domain.autoTransfer.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("AutoTransferScheduleRepository 테스트")
class AutoTransferScheduleRepositoryTest {

    @Autowired
    private AutoTransferScheduleRepository autoTransferScheduleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("특정 날짜에 유효한 활성 스케줄 조회")
    void findActiveSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        LocalDate validFrom = today.minusDays(10); // 10일 전부터 시작
        
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount, validFrom, null, true, false);

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findActiveSchedule(fromAccount, toAccount, today);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(schedule.getId());
    }

    @Test
    @DisplayName("유효 기간이 지난 스케줄은 조회되지 않음")
    void findActiveSchedule_ExpiredSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        LocalDate validFrom = today.minusDays(30);
        LocalDate validTo = today.minusDays(1); // 어제까지만 유효
        
        createAndSaveSchedule(fromAccount, toAccount, validFrom, validTo, true, false);

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findActiveSchedule(fromAccount, toAccount, today);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("비활성화된 스케줄은 조회되지 않음")
    void findActiveSchedule_InactiveSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        LocalDate validFrom = today.minusDays(10);
        
        createAndSaveSchedule(fromAccount, toAccount, validFrom, null, false, false); // 비활성화

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findActiveSchedule(fromAccount, toAccount, today);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 스케줄은 조회되지 않음")
    void findActiveSchedule_DeletedSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        LocalDate validFrom = today.minusDays(10);
        
        createAndSaveSchedule(fromAccount, toAccount, validFrom, null, true, true); // 삭제됨

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findActiveSchedule(fromAccount, toAccount, today);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("미래 스케줄 조회 - 가장 빠른 시작 날짜의 스케줄")
    void findFutureSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(10);
        
        AutoTransferSchedule schedule = createAndSaveSchedule(fromAccount, toAccount, futureDate, null, true, false);

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findFutureSchedule(fromAccount, toAccount, today);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(schedule.getId());
    }

    @Test
    @DisplayName("현재 날짜보다 이전 스케줄은 미래 스케줄로 조회되지 않음")
    void findFutureSchedule_PastSchedule() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        LocalDate pastDate = today.minusDays(10);
        
        createAndSaveSchedule(fromAccount, toAccount, pastDate, null, true, false);

        // When
        Optional<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findFutureSchedule(fromAccount, toAccount, today);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("머니박스 계좌를 목적지로 하는 모든 스케줄 조회")
    void findAllByToAccountAndDeletedFalse() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        Account otherToAccount = createAndSaveAccount(member, "11111111111", AccountType.MONEY_BOX);
        
        LocalDate today = LocalDate.now();
        
        AutoTransferSchedule schedule1 = createAndSaveSchedule(fromAccount, toAccount, today, null, true, false);
        AutoTransferSchedule schedule2 = createAndSaveSchedule(fromAccount, toAccount, today.plusDays(10), null, true, false);
        createAndSaveSchedule(fromAccount, otherToAccount, today, null, true, false); // 다른 계좌
        createAndSaveSchedule(fromAccount, toAccount, today.plusDays(20), null, true, true); // 삭제됨

        // When
        List<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findAllByToAccountAndDeletedFalse(toAccount);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AutoTransferSchedule::getId)
                .containsExactlyInAnyOrder(schedule1.getId(), schedule2.getId());
    }

    @Test
    @DisplayName("특정 이체일에 실행될 자동이체 스케줄 조회")
    void findSchedulesForExecution() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        LocalDate validFrom = LocalDate.of(2024, 3, 1);
        
        // 15일 이체 스케줄 생성
        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(validFrom)
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();
        autoTransferScheduleRepository.save(schedule);

        // When
        List<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findSchedulesForExecution(targetDate, 15);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(schedule.getId());
        assertThat(result.get(0).getTransferDay()).isEqualTo(15);
    }

    @Test
    @DisplayName("다른 이체일의 스케줄은 실행 대상에서 제외")
    void findSchedulesForExecution_DifferentTransferDay() {
        // Given
        Member member = createAndSaveMember();
        Account fromAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account toAccount = createAndSaveAccount(member, "98765432109", AccountType.MONEY_BOX);
        
        LocalDate targetDate = LocalDate.of(2024, 3, 15);
        LocalDate validFrom = LocalDate.of(2024, 3, 1);
        
        // 10일 이체 스케줄 생성 (15일과 다름)
        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(10)
                .validFrom(validFrom)
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();
        autoTransferScheduleRepository.save(schedule);

        // When
        List<AutoTransferSchedule> result = autoTransferScheduleRepository
                .findSchedulesForExecution(targetDate, 15);

        // Then
        assertThat(result).isEmpty();
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

    private AutoTransferSchedule createAndSaveSchedule(Account fromAccount, Account toAccount, 
                                                      LocalDate validFrom, LocalDate validTo, 
                                                      boolean active, boolean deleted) {
        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(100000))
                .transferDay(15)
                .validFrom(validFrom)
                .validTo(validTo)
                .active(active)
                .deleted(deleted)
                .build();
        return autoTransferScheduleRepository.save(schedule);
    }
}