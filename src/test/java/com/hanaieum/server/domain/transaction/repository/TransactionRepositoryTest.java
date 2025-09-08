package com.hanaieum.server.domain.transaction.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("TransactionRepository 테스트")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("계좌별 거래내역 페이징 조회 - 생성일시 내림차순")
    void findByAccountIdOrderByCreatedAtDesc() {
        // Given
        Member member = createAndSaveMember("010-1111-1111", "김하나");
        Account account = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        
        // 여러 거래 생성 (시간차를 두어 순서 확인)
        Transaction tx1 = createAndSaveTransaction(account, TransactionType.DEPOSIT, 
                new BigDecimal("10000"), "첫 번째 입금");
        
        Transaction tx2 = createAndSaveTransaction(account, TransactionType.WITHDRAW, 
                new BigDecimal("5000"), "첫 번째 출금");
        
        Transaction tx3 = createAndSaveTransaction(account, TransactionType.DEPOSIT, 
                new BigDecimal("20000"), "두 번째 입금");

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByAccountIdOrderByCreatedAtDesc(
                account.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        // 최신 거래가 먼저 오는지 확인 (내림차순)
        List<Transaction> transactions = result.getContent();
        assertThat(transactions.get(0).getDescription()).isEqualTo("두 번째 입금");
        assertThat(transactions.get(1).getDescription()).isEqualTo("첫 번째 출금");
        assertThat(transactions.get(2).getDescription()).isEqualTo("첫 번째 입금");
    }

    @Test
    @DisplayName("계좌별 거래내역 페이징 조회 - 페이지 크기 제한")
    void findByAccountIdOrderByCreatedAtDesc_WithPaging() {
        // Given
        Member member = createAndSaveMember("010-2222-2222", "이하나");
        Account account = createAndSaveAccount(member, "98765432109876", AccountType.MAIN);
        
        // 5개 거래 생성
        for (int i = 1; i <= 5; i++) {
            createAndSaveTransaction(account, TransactionType.DEPOSIT, 
                    new BigDecimal("10000"), "거래 " + i);
        }

        Pageable pageable = PageRequest.of(0, 3); // 페이지 크기 3

        // When
        Page<Transaction> result = transactionRepository.findByAccountIdOrderByCreatedAtDesc(
                account.getId(), pageable);

        // Then
        assertThat(result.getContent()).hasSize(3); // 페이지 크기만큼만 조회
        assertThat(result.getTotalElements()).isEqualTo(5); // 전체 요소는 5개
        assertThat(result.getTotalPages()).isEqualTo(2); // 총 2페이지
        assertThat(result.hasNext()).isTrue(); // 다음 페이지 존재
    }

    @Test
    @DisplayName("다른 계좌의 거래내역은 조회되지 않음")
    void findByAccountIdOrderByCreatedAtDesc_IsolatedByAccount() {
        // Given
        Member member1 = createAndSaveMember("010-1111-1111", "김하나");
        Member member2 = createAndSaveMember("010-2222-2222", "이하나");
        
        Account account1 = createAndSaveAccount(member1, "11111111111111", AccountType.MAIN);
        Account account2 = createAndSaveAccount(member2, "22222222222222", AccountType.MAIN);
        
        // 각 계좌에 거래 생성
        createAndSaveTransaction(account1, TransactionType.DEPOSIT, 
                new BigDecimal("10000"), "계좌1 거래");
        createAndSaveTransaction(account2, TransactionType.DEPOSIT, 
                new BigDecimal("20000"), "계좌2 거래");

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result1 = transactionRepository.findByAccountIdOrderByCreatedAtDesc(
                account1.getId(), pageable);
        Page<Transaction> result2 = transactionRepository.findByAccountIdOrderByCreatedAtDesc(
                account2.getId(), pageable);

        // Then
        assertThat(result1.getContent()).hasSize(1);
        assertThat(result1.getContent().get(0).getDescription()).isEqualTo("계좌1 거래");
        
        assertThat(result2.getContent()).hasSize(1);
        assertThat(result2.getContent().get(0).getDescription()).isEqualTo("계좌2 거래");
    }

    @Test
    @DisplayName("계좌와 거래타입별 조회 - 기준일시 이전, 생성일시 오름차순")
    void findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc() {
        // Given
        Member member = createAndSaveMember("010-3333-3333", "박하나");
        Account account = createAndSaveAccount(member, "33333333333333", AccountType.MAIN);
        
        // 현재 시간으로부터 충분히 미래의 시간을 기준일로 설정
        LocalDateTime targetDate = LocalDateTime.now().plusDays(1);
        
        // 거래들 생성 (모두 현재 시간으로 생성됨)
        createAndSaveTransaction(account, TransactionType.DEPOSIT, 
                new BigDecimal("10000"), "입금1");
        createAndSaveTransaction(account, TransactionType.WITHDRAW, 
                new BigDecimal("5000"), "출금1");
        createAndSaveTransaction(account, TransactionType.DEPOSIT, 
                new BigDecimal("15000"), "입금2");

        // When - DEPOSIT 타입만 조회 (현재 시간 < targetDate 이므로 모든 DEPOSIT 조회됨)
        List<Transaction> deposits = transactionRepository
                .findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc(
                        account, TransactionType.DEPOSIT, targetDate);

        // Then
        assertThat(deposits).hasSize(2); // DEPOSIT 2건만 조회
        assertThat(deposits.get(0).getDescription()).contains("입금"); // 입금 타입 확인
        assertThat(deposits.get(1).getDescription()).contains("입금"); // 입금 타입 확인
        
        // 모든 거래가 DEPOSIT 타입인지 확인
        assertThat(deposits.stream().allMatch(tx -> 
                tx.getTransactionType() == TransactionType.DEPOSIT)).isTrue();
    }

    @Test
    @DisplayName("계좌와 거래타입별 조회 - WITHDRAW만 조회")
    void findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc_WithdrawOnly() {
        // Given
        Member member = createAndSaveMember("010-4444-4444", "최하나");
        Account account = createAndSaveAccount(member, "44444444444444", AccountType.MAIN);
        
        // 미래 시간을 기준일로 설정
        LocalDateTime targetDate = LocalDateTime.now().plusDays(1);
        
        createAndSaveTransaction(account, TransactionType.DEPOSIT, 
                new BigDecimal("100000"), "입금");
        createAndSaveTransaction(account, TransactionType.WITHDRAW, 
                new BigDecimal("30000"), "출금1");
        createAndSaveTransaction(account, TransactionType.WITHDRAW, 
                new BigDecimal("20000"), "출금2");

        // When - WITHDRAW 타입만 조회
        List<Transaction> withdrawals = transactionRepository
                .findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc(
                        account, TransactionType.WITHDRAW, targetDate);

        // Then
        assertThat(withdrawals).hasSize(2); // WITHDRAW 2건만 조회
        assertThat(withdrawals.get(0).getDescription()).contains("출금"); // 출금 타입 확인
        assertThat(withdrawals.get(1).getDescription()).contains("출금"); // 출금 타입 확인
        
        // 모든 거래가 WITHDRAW 타입인지 확인
        assertThat(withdrawals.stream().allMatch(tx -> 
                tx.getTransactionType() == TransactionType.WITHDRAW)).isTrue();
    }

    // Helper methods
    private Member createAndSaveMember(String phoneNumber, String name) {
        Member member = Member.builder()
                .phoneNumber(phoneNumber)
                .password("encryptedPassword")
                .name(name)
                .gender(Gender.M)
                .birthDate(LocalDate.of(1990, 1, 1))
                .monthlyLivingCost(2000000)
                .mainAccountLinked(true)
                .hideGroupPrompt(false)
                .build();
        
        return memberRepository.save(member);
    }

    private Account createAndSaveAccount(Member member, String accountNumber, AccountType accountType) {
        Account account = Account.builder()
                .member(member)
                .number(accountNumber)
                .accountType(accountType)
                .name("테스트 계좌")
                .bankName("하나은행")
                .balance(new BigDecimal("1000000"))
                .password("encryptedPassword")
                .deleted(false)
                .build();
        
        return accountRepository.save(account);
    }

    private Transaction createAndSaveTransaction(Account account, TransactionType transactionType, 
                                               BigDecimal amount, String description) {
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(transactionType)
                .amount(amount)
                .balanceAfter(account.getBalance().add(
                        transactionType == TransactionType.DEPOSIT ? amount : amount.negate()))
                .counterpartyAccountId(999L)
                .counterpartyName("상대방")
                .description(description)
                .referenceType(ReferenceType.MANUAL)
                .referenceId(null)
                .build();
        
        return transactionRepository.save(transaction);
    }

}