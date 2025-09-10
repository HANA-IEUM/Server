package com.hanaieum.server.domain.transfer.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService 단위 테스트")
class TransferServiceImplTest {

    @Mock
    private AccountService accountService;
    
    @Mock
    private TransactionService transactionService;
    
    @Mock
    private BucketListRepository bucketListRepository;
    
    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    @DisplayName("머니박스 채우기 성공")
    void fillMoneyBox_Success() {
        // Given
        Long memberId = 1L;
        Long moneyBoxAccountId = 2L;
        Long mainAccountId = 1L;
        BigDecimal amount = new BigDecimal("50000");
        String password = "1234";

        Member member = createMember(memberId, "010-1111-1111", "김하나");
        Account mainAccount = createMainAccount(mainAccountId, member, new BigDecimal("100000"));
        Account moneyBoxAccount = createMoneyBoxAccount(moneyBoxAccountId, member, new BigDecimal("30000"));

        when(accountService.getMainAccountIdByMemberId(memberId)).thenReturn(mainAccountId);
        when(accountService.findByIdWithLock(mainAccountId)).thenReturn(mainAccount);
        when(accountService.findByIdWithLock(moneyBoxAccountId)).thenReturn(moneyBoxAccount);

        // When
        transferService.fillMoneyBox(memberId, moneyBoxAccountId, amount, password);

        // Then
        verify(accountService).getMainAccountIdByMemberId(memberId);
        verify(accountService).validateAccountOwnership(moneyBoxAccountId, memberId);
        verify(accountService).validateAccountPassword(mainAccountId, password);
        verify(accountService).debitBalance(mainAccount, amount);
        verify(accountService).creditBalance(moneyBoxAccount, amount);
        verify(transactionService).recordTransfer(
                eq(mainAccount), eq(moneyBoxAccount), eq(amount),
                eq(ReferenceType.MONEY_BOX_DEPOSIT), eq("머니박스 충전"), isNull()
        );
    }

    @Test
    @DisplayName("머니박스 채우기 실패 - 동일한 계좌로 이체")
    void fillMoneyBox_Fail_SameAccount() {
        // Given
        Long memberId = 1L;
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("50000");
        String password = "1234";

        when(accountService.getMainAccountIdByMemberId(memberId)).thenReturn(accountId);

        // When & Then
        assertThatThrownBy(() -> transferService.fillMoneyBox(memberId, accountId, amount, password))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRANSFER_SAME_ACCOUNT);

        verify(accountService).getMainAccountIdByMemberId(memberId);
        verify(accountService, never()).validateAccountOwnership(any(), any());
        verify(accountService, never()).validateAccountPassword(any(), any());
    }

    @Test
    @DisplayName("버킷 후원 성공")
    void sponsorBucket_Success() {
        // Given
        Long sponsorMemberId = 1L;
        Long bucketId = 1L;
        Long sponsorMainAccountId = 1L;
        Long moneyBoxAccountId = 2L;
        BigDecimal amount = new BigDecimal("30000");
        String password = "1234";

        Member sponsor = createMember(sponsorMemberId, "010-1111-1111", "후원자");
        Member bucketOwner = createMember(2L, "010-2222-2222", "버킷소유자");

        Account sponsorMainAccount = createMainAccount(sponsorMainAccountId, sponsor, new BigDecimal("200000"));
        Account moneyBoxAccount = createMoneyBoxAccount(moneyBoxAccountId, bucketOwner, new BigDecimal("50000"));

        BucketList bucketList = createBucketList(bucketId, bucketOwner, moneyBoxAccount);

        when(accountService.getMainAccountIdByMemberId(sponsorMemberId)).thenReturn(sponsorMainAccountId);
        when(bucketListRepository.findByIdAndDeletedFalse(bucketId)).thenReturn(Optional.of(bucketList));
        when(accountService.findByIdWithLock(sponsorMainAccountId)).thenReturn(sponsorMainAccount);
        when(accountService.findByIdWithLock(moneyBoxAccountId)).thenReturn(moneyBoxAccount);

        // When
        transferService.sponsorBucket(sponsorMemberId, bucketId, amount, password);

        // Then
        verify(accountService).getMainAccountIdByMemberId(sponsorMemberId);
        verify(bucketListRepository).findByIdAndDeletedFalse(bucketId);
        verify(accountService).validateAccountPassword(sponsorMainAccountId, password);
        verify(accountService).debitBalance(sponsorMainAccount, amount);
        verify(accountService).creditBalance(moneyBoxAccount, amount);
        verify(transactionService).recordTransfer(
                eq(sponsorMainAccount), eq(moneyBoxAccount), eq(amount),
                eq(ReferenceType.BUCKET_FUNDING), eq("후원"), eq(bucketId)
        );
    }

    @Test
    @DisplayName("버킷 후원 실패 - 버킷리스트를 찾을 수 없음")
    void sponsorBucket_Fail_BucketNotFound() {
        // Given
        Long sponsorMemberId = 1L;
        Long bucketId = 999L;
        Long sponsorMainAccountId = 1L;
        BigDecimal amount = new BigDecimal("30000");
        String password = "1234";

        when(accountService.getMainAccountIdByMemberId(sponsorMemberId)).thenReturn(sponsorMainAccountId);
        when(bucketListRepository.findByIdAndDeletedFalse(bucketId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transferService.sponsorBucket(sponsorMemberId, bucketId, amount, password))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("버킷리스트를 찾을 수 없습니다");

        verify(accountService).getMainAccountIdByMemberId(sponsorMemberId);
        verify(bucketListRepository).findByIdAndDeletedFalse(bucketId);
        verify(accountService, never()).validateAccountPassword(any(), any());
        verify(transactionService, never()).recordTransfer(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("자동이체 실행 성공")
    void executeAutoTransfer_Success() {
        // Given
        Long fromAccountId = 1L;
        Long toAccountId = 2L;
        BigDecimal amount = new BigDecimal("100000");
        Long scheduleId = 10L;

        Member fromMember = createMember(1L, "010-1111-1111", "출금자");
        Member toMember = createMember(2L, "010-2222-2222", "입금자");
        
        Account fromAccount = createMainAccount(fromAccountId, fromMember, new BigDecimal("500000"));
        Account toAccount = createMoneyBoxAccount(toAccountId, toMember, new BigDecimal("100000"));

        when(accountService.findByIdWithLock(fromAccountId)).thenReturn(fromAccount);
        when(accountService.findByIdWithLock(toAccountId)).thenReturn(toAccount);

        // When
        transferService.executeAutoTransfer(fromAccountId, toAccountId, amount, scheduleId);

        // Then
        verify(accountService).findByIdWithLock(fromAccountId);
        verify(accountService).findByIdWithLock(toAccountId);
        verify(accountService).debitBalance(fromAccount, amount);
        verify(accountService).creditBalance(toAccount, amount);
        verify(transactionService).recordTransfer(
                eq(fromAccount), eq(toAccount), eq(amount),
                eq(ReferenceType.AUTO_TRANSFER), eq("자동이체"), eq(scheduleId)
        );
        
        // 자동이체는 비밀번호 검증 없음
        verify(accountService, never()).validateAccountPassword(any(), any());
    }

    @Test
    @DisplayName("머니박스 전액 인출 성공 - 잔액 있음")
    void withdrawAllFromMoneyBox_Success_WithBalance() {
        // Given
        Long memberId = 1L;
        Long moneyBoxAccountId = 2L;
        Long mainAccountId = 1L;
        Long referenceId = 1L;
        BigDecimal moneyBoxBalance = new BigDecimal("150000");

        Member member = createMember(memberId, "010-1111-1111", "김하나");
        Account mainAccount = createMainAccount(mainAccountId, member, new BigDecimal("100000"));
        Account moneyBoxAccount = createMoneyBoxAccount(moneyBoxAccountId, member, moneyBoxBalance);

        when(accountService.getMainAccountIdByMemberId(memberId)).thenReturn(mainAccountId);
        when(accountService.findByIdWithLock(moneyBoxAccountId)).thenReturn(moneyBoxAccount);
        when(accountService.findByIdWithLock(mainAccountId)).thenReturn(mainAccount);

        // When
        BigDecimal withdrawnAmount = transferService.withdrawAllFromMoneyBox(memberId, moneyBoxAccountId, referenceId);

        // Then
        assertThat(withdrawnAmount).isEqualByComparingTo(moneyBoxBalance);
        
        verify(accountService).getMainAccountIdByMemberId(memberId);
        verify(accountService, times(2)).findByIdWithLock(moneyBoxAccountId);
        verify(accountService).findByIdWithLock(mainAccountId);
        verify(accountService).debitBalance(moneyBoxAccount, moneyBoxBalance);
        verify(accountService).creditBalance(mainAccount, moneyBoxBalance);
        verify(transactionService).recordTransfer(
                eq(moneyBoxAccount), eq(mainAccount), eq(moneyBoxBalance),
                eq(ReferenceType.MONEY_BOX_WITHDRAW), eq("머니박스 원금 인출"), eq(referenceId)
        );
    }

    @Test
    @DisplayName("머니박스 전액 인출 성공 - 잔액 0원")
    void withdrawAllFromMoneyBox_Success_NoBalance() {
        // Given
        Long memberId = 1L;
        Long moneyBoxAccountId = 2L;
        Long mainAccountId = 1L;
        Long referenceId = 1L;

        Member member = createMember(memberId, "010-1111-1111", "김하나");
        Account moneyBoxAccount = createMoneyBoxAccount(moneyBoxAccountId, member, BigDecimal.ZERO);

        when(accountService.getMainAccountIdByMemberId(memberId)).thenReturn(mainAccountId);
        when(accountService.findByIdWithLock(moneyBoxAccountId)).thenReturn(moneyBoxAccount);

        // When
        BigDecimal withdrawnAmount = transferService.withdrawAllFromMoneyBox(memberId, moneyBoxAccountId, referenceId);

        // Then
        assertThat(withdrawnAmount).isEqualByComparingTo(BigDecimal.ZERO);
        
        verify(accountService).getMainAccountIdByMemberId(memberId);
        verify(accountService).findByIdWithLock(moneyBoxAccountId);
        // 잔액이 0이므로 이체 실행되지 않음
        verify(accountService, never()).debitBalance(any(), any());
        verify(accountService, never()).creditBalance(any(), any());
        verify(transactionService, never()).recordTransfer(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이자 지급 성공")
    void payInterest_Success() {
        // Given
        Long memberId = 1L;
        BigDecimal interestAmount = new BigDecimal("5000");
        Long bucketListId = 1L;
        Long mainAccountId = 1L;

        Member member = createMember(memberId, "010-1111-1111", "김하나");
        Account mainAccount = createMainAccount(mainAccountId, member, new BigDecimal("100000"));

        when(accountService.getMainAccountIdByMemberId(memberId)).thenReturn(mainAccountId);
        when(accountService.findByIdWithLock(mainAccountId)).thenReturn(mainAccount);

        // When
        transferService.payInterest(memberId, interestAmount, bucketListId);

        // Then
        verify(accountService).getMainAccountIdByMemberId(memberId);
        verify(accountService).findByIdWithLock(mainAccountId);
        verify(accountService).creditBalance(mainAccount, interestAmount);
        verify(transactionService).recordDeposit(
                eq(mainAccount), eq(interestAmount), isNull(), eq("하나이음"),
                eq(ReferenceType.MONEY_BOX_INTEREST), eq("머니박스 이자"), eq(bucketListId)
        );
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
                .mainAccountLinked(true)
                .hideGroupPrompt(false)
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

    private BucketList createBucketList(Long id, Member member, Account moneyBoxAccount) {
        return BucketList.builder()
                .id(id)
                .member(member)
                .type(BucketListType.TRIP)
                .title("여행 가자!")
                .targetAmount(new BigDecimal("1000000"))
                .targetDate(LocalDate.now().plusMonths(6))
                .publicFlag(true)
                .shareFlag(false)
                .moneyBoxAccount(moneyBoxAccount)
                .deleted(false)
                .build();
    }
}