package com.hanaieum.server.domain.account.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.dto.MainAccountResponse;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {
    
    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public Long createAccount(Member member, String accountName, String bankName, AccountType accountType, Long balance, String password) {
        // 유니크한 계좌번호 생성
        String accountNumber = generateUniqueAccountNumber(accountType);
        
        // 계좌 비밀번호 암호화
        String encodedAccountPassword = passwordEncoder.encode(password);
        
        Account account = Account.builder()
                .member(member)
                .number(accountNumber)
                .name(accountName)
                .bankName(bankName)
                .password(encodedAccountPassword)
                .balance(balance)
                .accountType(accountType)
                .deleted(false)
                .build();
                
        Account savedAccount = accountRepository.save(account);
        log.info("계좌 생성 완료 - 회원 ID: {}, 계좌번호: {}, 계좌명: {}, 은행: {}, 타입: {}", 
                member.getId(), accountNumber, accountName, bankName, accountType);
        
        return savedAccount.getId();
    }
    
    @Override
    public Long createMainAccount(Member member) {
        // 통장 이름 목록
        List<String> accountNames = Arrays.asList(
            "주거래하나 통장",
            "하나 플러스 통장", 
            "하나더넥스트 연금 통장"
        );
        
        SecureRandom random = new SecureRandom();
        String randomAccountName = accountNames.get(random.nextInt(accountNames.size()));
        
        // 주계좌 생성 (잔액: 7천만원, 비밀번호: 1234)
        return createAccount(member, randomAccountName, "하나은행", AccountType.MAIN, 70000000L, "1234");
    }
    
    @Override
    public Long createAccount(Long memberId, String accountName, String bankName, AccountType accountType, Long balance, String password) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        return createAccount(member, accountName, bankName, accountType, balance, password);
    }
    
    @Override
    public Long createMoneyBoxAccount(Long memberId, String accountName) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        // 독립 실행용 - 잔액 0원, 비밀번호 1234
        return createAccount(member, accountName, "하나은행", AccountType.MONEY_BOX, 0L, "1234");
    }
    
    @Override
    public Long createMoneyBoxAccount(Member member, String accountName) {
        // 연계 실행용 - 잔액 0원, 비밀번호 1234 
        return createAccount(member, accountName, "하나은행", AccountType.MONEY_BOX, 0L, "1234");
    }

    // AccountType.MAIN (주계좌) : 14자리 숫자 (XXX-ZZZZZZ-ZZCYY)
    // AccountType.MONEY_BOX (머니박스계좌) : 11자리 숫자 (XXX-YY-ZZZZZ-C)
    private String generateUniqueAccountNumber(AccountType accountType) {
        SecureRandom random = new SecureRandom();
        String accountNumber;
        
        int length = (accountType == AccountType.MAIN) ? 14 : 11;
        
        do {
            StringBuilder sb = new StringBuilder();
            
            // 지정된 길이만큼 랜덤 숫자 생성
            for (int i = 0; i < length; i++) {
                sb.append(random.nextInt(10));
            }
            
            accountNumber = sb.toString();
            
        } while (accountRepository.existsByNumber(accountNumber));
        
        return accountNumber;
    }

    @Override
    @Transactional(readOnly = true)
    public MainAccountResponse getMainAccount(Member member) {
        Account mainAccount = accountRepository.findByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        log.info("주계좌 조회 완료 - 회원 ID: {}, 계좌번호: {}", member.getId(), mainAccount.getNumber());
        
        return MainAccountResponse.of(mainAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getMainAccountByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        Account mainAccount = accountRepository.findByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        log.info("주계좌 조회 완료 - 회원 ID: {}, 계좌 ID: {}", memberId, mainAccount.getId());
        
        return mainAccount;
    }

    @Override
    @Transactional(readOnly = true)
    public Account findById(Long accountId) {
        return accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    public Account findByIdWithLock(Long accountId) {
        return accountRepository.findByIdAndDeletedFalseWithLock(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAccountOwnership(Long accountId, Long memberId) {
        Account account = findById(accountId);
        if (!account.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAccountPassword(Long accountId, String password) {
        Account account = findById(accountId);
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateSufficientBalance(Long accountId, BigDecimal amount) {
        Account account = findById(accountId);
        if (account.getBalance() < amount.longValue()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    @Override
    public void debitBalance(Long accountId, BigDecimal amount) {
        Account account = findByIdWithLock(accountId);
        long newBalance = account.getBalance() - amount.longValue();
        if (newBalance < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        account.updateBalance(newBalance);
        accountRepository.save(account);
        log.info("출금 처리 완료 - 계좌 ID: {}, 출금액: {}, 잔액: {}", accountId, amount, newBalance);
    }

    @Override
    public void creditBalance(Long accountId, BigDecimal amount) {
        Account account = findByIdWithLock(accountId);
        long newBalance = account.getBalance() + amount.longValue();
        account.updateBalance(newBalance);
        accountRepository.save(account);
        log.info("입금 처리 완료 - 계좌 ID: {}, 입금액: {}, 잔액: {}", accountId, amount, newBalance);
    }

}