package com.hanaieum.server.domain.transaction.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.transaction.dto.TransactionResponse;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import com.hanaieum.server.domain.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService 단위 테스트")
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountService accountService;
    
    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    @DisplayName("이체 거래 기록 생성 성공 - 이중 기입 방식")
    void recordTransfer_Success() {
        // Given
        Member fromMember = createMember(1L, "010-1111-1111", "김하나");
        Member toMember = createMember(2L, "010-2222-2222", "이하나");
        
        Account fromAccount = createMainAccount(1L, fromMember, new BigDecimal("100000"));
        Account toAccount = createMoneyBoxAccount(2L, toMember, new BigDecimal("50000"));
        
        BigDecimal amount = new BigDecimal("10000");
        ReferenceType referenceType = ReferenceType.MONEY_BOX_DEPOSIT;
        String description = "머니박스 충전";
        Long referenceId = 1L;

        Transaction withdrawTx = createTransaction(1L, fromAccount, TransactionType.WITHDRAW, 
                amount, new BigDecimal("90000"), toAccount.getId(), toMember.getName(), 
                description, referenceType, referenceId);
        
        Transaction depositTx = createTransaction(2L, toAccount, TransactionType.DEPOSIT, 
                amount, new BigDecimal("60000"), fromAccount.getId(), fromMember.getName(), 
                description, referenceType, referenceId);

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(withdrawTx)
                .thenReturn(depositTx);

        // When
        transactionService.recordTransfer(fromAccount, toAccount, amount, 
                referenceType, description, referenceId);

        // Then
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        
        // 출금 레코드 검증
        verify(transactionRepository).save(argThat(tx -> 
                tx.getAccount().equals(fromAccount) &&
                tx.getTransactionType().equals(TransactionType.WITHDRAW) &&
                tx.getAmount().equals(amount) &&
                tx.getBalanceAfter().equals(fromAccount.getBalance()) &&
                tx.getCounterpartyAccountId().equals(toAccount.getId()) &&
                tx.getCounterpartyName().equals(toMember.getName()) &&
                tx.getDescription().equals(description) &&
                tx.getReferenceType().equals(referenceType) &&
                tx.getReferenceId().equals(referenceId)
        ));
        
        // 입금 레코드 검증
        verify(transactionRepository).save(argThat(tx -> 
                tx.getAccount().equals(toAccount) &&
                tx.getTransactionType().equals(TransactionType.DEPOSIT) &&
                tx.getAmount().equals(amount) &&
                tx.getBalanceAfter().equals(toAccount.getBalance()) &&
                tx.getCounterpartyAccountId().equals(fromAccount.getId()) &&
                tx.getCounterpartyName().equals(fromMember.getName()) &&
                tx.getDescription().equals(description) &&
                tx.getReferenceType().equals(referenceType) &&
                tx.getReferenceId().equals(referenceId)
        ));
    }

    @Test
    @DisplayName("입금 거래 기록 생성 성공")
    void recordDeposit_Success() {
        // Given
        Member member = createMember(1L, "010-1111-1111", "김하나");
        Account account = createMainAccount(1L, member, new BigDecimal("100000"));
        
        BigDecimal amount = new BigDecimal("5000");
        Long counterpartyAccountId = null;
        String counterpartyName = "하나이음";
        ReferenceType referenceType = ReferenceType.MONEY_BOX_INTEREST;
        String description = "목표달성 이자지급";
        Long referenceId = 1L;

        Transaction depositTx = createTransaction(1L, account, TransactionType.DEPOSIT, 
                amount, new BigDecimal("105000"), counterpartyAccountId, counterpartyName, 
                description, referenceType, referenceId);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(depositTx);

        // When
        transactionService.recordDeposit(account, amount, counterpartyAccountId, 
                counterpartyName, referenceType, description, referenceId);

        // Then
        verify(transactionRepository).save(argThat(tx -> 
                tx.getAccount().equals(account) &&
                tx.getTransactionType().equals(TransactionType.DEPOSIT) &&
                tx.getAmount().equals(amount) &&
                tx.getBalanceAfter().equals(account.getBalance()) &&
                Objects.equals(tx.getCounterpartyAccountId(), counterpartyAccountId) &&
                tx.getCounterpartyName().equals(counterpartyName) &&
                tx.getDescription().equals(description) &&
                tx.getReferenceType().equals(referenceType) &&
                Objects.equals(tx.getReferenceId(), referenceId)
        ));
    }

    @Test
    @DisplayName("거래 내역 조회 성공 - 페이징")
    void getTransactionsByAccountId_Success() {
        // Given
        Long memberId = 1L;
        Long accountId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        Member member = createMember(memberId, "010-1111-1111", "김하나");
        Account account = createMainAccount(accountId, member, new BigDecimal("100000"));
        
        List<Transaction> transactions = Arrays.asList(
                createTransaction(1L, account, TransactionType.DEPOSIT, 
                        new BigDecimal("50000"), new BigDecimal("150000"), 
                        2L, "이하나", "머니박스 후원", ReferenceType.BUCKET_FUNDING, 1L),
                createTransaction(2L, account, TransactionType.WITHDRAW, 
                        new BigDecimal("20000"), new BigDecimal("80000"), 
                        3L, "박하나", "머니박스 충전", ReferenceType.MONEY_BOX_DEPOSIT, 2L)
        );
        
        Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, transactions.size());
        
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable))
                .thenReturn(transactionPage);

        // When
        Page<TransactionResponse> result = transactionService.getTransactionsByAccountId(
                memberId, accountId, pageable);

        // Then
        verify(accountService).validateAccountOwnership(accountId, memberId);
        verify(transactionRepository).findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        // 첫 번째 거래 검증
        TransactionResponse firstResponse = result.getContent().get(0);
        assertThat(firstResponse.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(firstResponse.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(firstResponse.getCounterpartyName()).isEqualTo("이하나");
        assertThat(firstResponse.getDescription()).isEqualTo("머니박스 후원");
    }

    @Test
    @DisplayName("거래 타입별 조회 성공")
    void getTransactionsByTransactionType_Success() {
        // Given
        Member member = createMember(1L, "010-1111-1111", "김하나");
        Account account = createMainAccount(1L, member, new BigDecimal("100000"));
        TransactionType transactionType = TransactionType.DEPOSIT;
        LocalDate targetDate = LocalDate.now();
        
        List<Transaction> transactions = Arrays.asList(
                createTransaction(1L, account, TransactionType.DEPOSIT, 
                        new BigDecimal("30000"), new BigDecimal("130000"), 
                        2L, "이하나", "입금", ReferenceType.MANUAL, null),
                createTransaction(2L, account, TransactionType.DEPOSIT, 
                        new BigDecimal("20000"), new BigDecimal("150000"), 
                        3L, "박하나", "입금", ReferenceType.AUTO_TRANSFER, 1L)
        );
        
        when(transactionRepository.findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc(
                account, transactionType, targetDate.atStartOfDay()))
                .thenReturn(transactions);

        // When
        List<Transaction> result = transactionService.getTransactionsByTransactionType(
                account, transactionType, targetDate);

        // Then
        verify(transactionRepository).findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc(
                account, transactionType, targetDate.atStartOfDay());
        
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.get(1).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    // Helper methods
    private Member createMember(Long id, String phoneNumber, String name) {
        return Member.builder()
                .id(id)
                .phoneNumber(phoneNumber)
                .password("encryptedPassword")
                .name(name)
                .gender(Gender.M)
                .birthDate(LocalDate.of(1990, 1, 1))
                .monthlyLivingCost(2000000)
                .build();
    }

    private Account createMainAccount(Long id, Member member, BigDecimal balance) {
        return Account.builder()
                .id(id)
                .member(member)
                .number("12345678901234")
                .accountType(AccountType.MAIN)
                .name("주거래하나 통장")
                .bankName("하나은행")
                .balance(balance)
                .password("encryptedPassword")
                .deleted(false)
                .build();
    }

    private Account createMoneyBoxAccount(Long id, Member member, BigDecimal balance) {
        return Account.builder()
                .id(id)
                .member(member)
                .number("12345678901")
                .accountType(AccountType.MONEY_BOX)
                .name("머니박스")
                .bankName("하나은행")
                .balance(balance)
                .password("encryptedPassword")
                .boxName("여행 머니박스")
                .deleted(false)
                .build();
    }

    private Transaction createTransaction(Long id, Account account, TransactionType transactionType,
                                       BigDecimal amount, BigDecimal balanceAfter, Long counterpartyAccountId,
                                       String counterpartyName, String description, ReferenceType referenceType,
                                       Long referenceId) {
        return Transaction.builder()
                .id(id)
                .account(account)
                .transactionType(transactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .counterpartyAccountId(counterpartyAccountId)
                .counterpartyName(counterpartyName)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
    }
}