package com.hanaieum.server.domain.account.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.autoTransfer.service.AutoTransferScheduleService;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 단위 테스트")
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AutoTransferScheduleService autoTransferScheduleService;
    
    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    @DisplayName("범용 계좌 생성 성공")
    void createAccount_Success() {
        // Given
        Member member = createTestMember();
        String accountName = "테스트 계좌";
        String bankName = "하나은행";
        AccountType accountType = AccountType.MAIN;
        BigDecimal balance = new BigDecimal("10000");
        String password = "1234";
        
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(accountRepository.existsByNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // When
        Long accountId = accountService.createAccount(member, accountName, bankName, accountType, balance, password);

        // Then
        assertThat(accountId).isEqualTo(1L);
        verify(passwordEncoder).encode(password);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("연금 통장 생성 - 1965년 이하 출생자")
    void createMainAccount_SeniorPensionAccount() {
        // Given
        Member seniorMember = Member.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .name("김시니어")
                .birthDate(LocalDate.of(1960, 1, 1))
                .build();

        when(passwordEncoder.encode("1234")).thenReturn("encoded_1234");
        when(accountRepository.existsByNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // When
        Long accountId = accountService.createMainAccount(seniorMember);

        // Then
        assertThat(accountId).isEqualTo(1L);
        verify(accountRepository).save(argThat(account -> 
            account.getName().equals("하나더넥스트 연금 통장") &&
            account.getAccountType() == AccountType.MAIN &&
            account.getBalance().equals(new BigDecimal("70000000"))
        ));
    }

    @Test
    @DisplayName("일반 통장 생성 - 1966년 이상 출생자")
    void createMainAccount_RegularAccount() {
        // Given
        Member youngMember = Member.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .name("김청년")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        when(passwordEncoder.encode("1234")).thenReturn("encoded_1234");
        when(accountRepository.existsByNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        // When
        Long accountId = accountService.createMainAccount(youngMember);

        // Then
        assertThat(accountId).isEqualTo(1L);
        verify(accountRepository).save(argThat(account -> 
            (account.getName().equals("주거래하나 통장") || account.getName().equals("하나 플러스 통장")) &&
            account.getAccountType() == AccountType.MAIN &&
            account.getBalance().equals(new BigDecimal("70000000"))
        ));
    }

    @Test
    @DisplayName("머니박스 계좌 생성 성공")
    void createMoneyBoxAccount_Success() {
        // Given
        Member member = createTestMember();
        String boxName = "여행 머니박스";
        String password = "1234";

        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(accountRepository.existsByNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(2L);
            return account;
        });

        // When
        Account account = accountService.createMoneyBoxAccount(member, boxName, password);

        // Then
        assertThat(account.getId()).isEqualTo(2L);
        assertThat(account.getBoxName()).isEqualTo(boxName);
        assertThat(account.getAccountType()).isEqualTo(AccountType.MONEY_BOX);
        assertThat(account.getBalance()).isEqualTo(BigDecimal.ZERO);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("버킷리스트 연동 머니박스 생성 - 박스명 지정")
    void createMoneyBoxForBucketList_WithBoxName() {
        // Given
        Member member = createTestMember();
        BucketList bucketList = createTestBucketList();
        String boxName = "커스텀 머니박스";

        when(passwordEncoder.encode("1234")).thenReturn("encoded_1234");
        when(accountRepository.existsByNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(3L);
            return account;
        });

        // When
        Account account = accountService.createMoneyBoxForBucketList(bucketList, member, boxName);

        // Then
        assertThat(account.getBoxName()).isEqualTo(boxName);
        assertThat(bucketList.getMoneyBoxAccount()).isEqualTo(account);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("버킷리스트 연동 머니박스 생성 - 박스명 미지정시 버킷리스트 제목 사용")
    void createMoneyBoxForBucketList_WithoutBoxName() {
        // Given
        Member member = createTestMember();
        BucketList bucketList = createTestBucketList();

        when(passwordEncoder.encode("1234")).thenReturn("encoded_1234");
        when(accountRepository.existsByNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(3L);
            return account;
        });

        // When
        Account account = accountService.createMoneyBoxForBucketList(bucketList, member, null);

        // Then
        assertThat(account.getBoxName()).isEqualTo(bucketList.getTitle());
        verify(accountRepository).save(any(Account.class));
    }



    @Test
    @DisplayName("계좌 소유권 검증 성공")
    void validateAccountOwnership_Success() {
        // Given
        Long accountId = 1L;
        Long memberId = 1L;
        Member member = createTestMember();
        Account account = createTestMainAccount(member);
        
        when(accountRepository.findByIdAndDeletedFalse(accountId))
                .thenReturn(Optional.of(account));

        // When & Then
        assertThatNoException().isThrownBy(() -> 
            accountService.validateAccountOwnership(accountId, memberId));
    }

    @Test
    @DisplayName("계좌 소유권 검증 실패")
    void validateAccountOwnership_AccessDenied() {
        // Given
        Long accountId = 1L;
        Long memberId = 1L;
        Long otherMemberId = 2L;
        
        Member member = createTestMember();
        Member otherMember = Member.builder().id(otherMemberId).build();
        Account account = createTestMainAccount(otherMember);
        
        when(accountRepository.findByIdAndDeletedFalse(accountId))
                .thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> 
            accountService.validateAccountOwnership(accountId, memberId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ACCOUNT_ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("계좌 비밀번호 검증 성공")
    void validateAccountPassword_Success() {
        // Given
        Long accountId = 1L;
        String password = "1234";
        Member member = createTestMember();
        Account account = createTestMainAccount(member);
        
        when(accountRepository.findByIdAndDeletedFalse(accountId))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches(password, account.getPassword())).thenReturn(true);

        // When & Then
        assertThatNoException().isThrownBy(() -> 
            accountService.validateAccountPassword(accountId, password));
    }

    @Test
    @DisplayName("계좌 비밀번호 검증 실패")
    void validateAccountPassword_InvalidPassword() {
        // Given
        Long accountId = 1L;
        String password = "wrong";
        Member member = createTestMember();
        Account account = createTestMainAccount(member);
        
        when(accountRepository.findByIdAndDeletedFalse(accountId))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches(password, account.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> 
            accountService.validateAccountPassword(accountId, password))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_ACCOUNT_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("출금 처리 성공")
    void debitBalance_Success() {
        // Given
        BigDecimal amount = new BigDecimal("3000");
        Member member = createTestMember();
        Account account = Account.builder()
                .id(1L)
                .member(member)
                .balance(new BigDecimal("10000"))
                .build();

        when(accountRepository.save(account)).thenReturn(account);

        // When
        accountService.debitBalance(account, amount);

        // Then
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("7000"));
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("출금 처리 실패 - 잔액 부족")
    void debitBalance_InsufficientBalance() {
        // Given
        BigDecimal amount = new BigDecimal("15000");
        Member member = createTestMember();
        Account account = Account.builder()
                .id(1L)
                .member(member)
                .balance(new BigDecimal("10000"))
                .build();

        // When & Then
        assertThatThrownBy(() -> accountService.debitBalance(account, amount))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_BALANCE.getMessage());

        verify(accountRepository, never()).save(account);
    }

    @Test
    @DisplayName("입금 처리 성공")
    void creditBalance_Success() {
        // Given
        BigDecimal amount = new BigDecimal("5000");
        Member member = createTestMember();
        Account account = Account.builder()
                .id(1L)
                .member(member)
                .balance(new BigDecimal("10000"))
                .build();

        when(accountRepository.save(account)).thenReturn(account);

        // When
        accountService.creditBalance(account, amount);

        // Then
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("15000"));
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("머니박스 개수 조회")
    void getMoneyBoxCountByMember() {
        // Given
        Member member = createTestMember();
        long expectedCount = 3L;
        
        when(accountRepository.countByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX))
                .thenReturn(expectedCount);

        // When
        long actualCount = accountService.getMoneyBoxCountByMember(member);

        // Then
        assertThat(actualCount).isEqualTo(expectedCount);
        verify(accountRepository).countByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
    }



    @Test
    @DisplayName("머니박스 목록 조회 - 성공")
    void findMoneyBoxes_Success() {
        // Given
        Member member = createTestMember();
        List<Account> moneyBoxes = Arrays.asList(
            createTestMoneyBox(member, 1L, "11111111111", "여행 머니박스"),
            createTestMoneyBox(member, 2L, "22222222222", "쇼핑 머니박스"),
            createTestMoneyBox(member, 3L, "33333333333", "생활비 머니박스")
        );
        
        when(accountRepository.findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX))
                .thenReturn(moneyBoxes);

        // When
        List<Account> result = accountService.findMoneyBoxes(member);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(account -> account.getAccountType() == AccountType.MONEY_BOX);
        assertThat(result).allMatch(account -> account.getNumber().length() == 11); // 머니박스는 11자리
        assertThat(result).allMatch(account -> account.getBoxName() != null); // 머니박스는 boxName 필수
        assertThat(result).extracting(Account::getBoxName)
                .containsExactly("여행 머니박스", "쇼핑 머니박스", "생활비 머니박스");
        verify(accountRepository).findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
    }

    @Test
    @DisplayName("머니박스 목록 조회 - 빈 목록")
    void findMoneyBoxes_EmptyList() {
        // Given
        Member member = createTestMember();
        
        when(accountRepository.findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX))
                .thenReturn(Arrays.asList());

        // When
        List<Account> result = accountService.findMoneyBoxes(member);

        // Then
        assertThat(result).isEmpty();
        verify(accountRepository).findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
    }


    // Helper methods
    private Member createTestMember() {
        return Member.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .name("테스트유저")
                .birthDate(LocalDate.of(1990, 1, 1))
                .mainAccountLinked(true)
                .build();
    }

    private Account createTestMainAccount(Member member) {
        return Account.builder()
                .id(1L)
                .member(member)
                .number("12345678901234")
                .name("주거래하나 통장")
                .bankName("하나은행")
                .password("encoded_password")
                .balance(new BigDecimal("70000000"))
                .accountType(AccountType.MAIN)
                .deleted(false)
                .build();
    }

    private BucketList createTestBucketList() {
        return BucketList.builder()
                .id(1L)
                .title("제주도 여행")
                .build();
    }

    private Account createTestMoneyBox(Member member, Long id, String accountNumber, String boxName) {
        return Account.builder()
                .id(id)
                .member(member)
                .number(accountNumber)
                .name("하나머니박스")
                .bankName("하나은행")
                .password("encoded_password")
                .balance(BigDecimal.ZERO)
                .accountType(AccountType.MONEY_BOX)
                .boxName(boxName)
                .deleted(false)
                .build();
    }
}