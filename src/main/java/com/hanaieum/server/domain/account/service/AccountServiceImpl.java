package com.hanaieum.server.domain.account.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.dto.MainAccountResponse;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.autoTransfer.service.AutoTransferScheduleService;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    
    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutoTransferScheduleService autoTransferScheduleService;
    
    @Override
    @Transactional
    public Long createAccount(Member member, String accountName, String bankName, AccountType accountType, BigDecimal balance, String password) {
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
    @Transactional
    public Long createMainAccount(Member member) {
        String accountName;
        
        // 1965년 이하 출생자(65세 이상)는 연금통장 고정
        int birthYear = member.getBirthDate().getYear();
        if (birthYear <= 1965) {
            accountName = "하나더넥스트 연금 통장";
        } else {
            // 일반 통장 이름 목록 중 랜덤 선택
            List<String> accountNames = Arrays.asList(
                "주거래하나 통장",
                "하나 플러스 통장"
            );
            
            SecureRandom random = new SecureRandom();
            accountName = accountNames.get(random.nextInt(accountNames.size()));
        }
        
        // 주계좌 생성 (잔액: 7천만원, 비밀번호: 1234)
        return createAccount(member, accountName, "하나은행", AccountType.MAIN, new BigDecimal("70000000"), "1234");
    }
    
    @Override
    @Transactional
    public Account createMoneyBoxAccount(Member member, String boxName, String password) {
        // 머니박스 계좌 생성 - 잔액 0원
        String accountNumber = generateUniqueAccountNumber(AccountType.MONEY_BOX);
        String encodedAccountPassword = passwordEncoder.encode(password);
        
        Account account = Account.builder()
                .member(member)
                .number(accountNumber)
                .name("하나머니박스")
                .bankName("하나은행")
                .password(encodedAccountPassword)
                .balance(BigDecimal.ZERO)
                .accountType(AccountType.MONEY_BOX)
                .boxName(boxName)
                .deleted(false)
                .build();
                
        Account savedAccount = accountRepository.save(account);
        log.info("머니박스 계좌 생성 완료 - 회원 ID: {}, 계좌번호: {}, 박스명: {}", 
                member.getId(), accountNumber, boxName);
        
        return savedAccount;
    }
    
    @Override
    @Transactional
    public Account createMoneyBoxForBucketList(BucketList bucketList, Member member, String boxName) {
        // boxName이 없으면 버킷리스트 제목 사용
        String finalBoxName = (boxName != null && !boxName.trim().isEmpty()) ? boxName : bucketList.getTitle();
        
        // 머니박스 계좌 생성 (기본 비밀번호: 1234)
        Account account = createMoneyBoxAccount(member, finalBoxName, "1234");
        
        // BucketList와 Account 양방향 연결
        bucketList.setMoneyBoxAccount(account);
        
        log.info("버킷리스트 연동 머니박스 생성 완료 - 버킷리스트 ID: {}, 계좌 ID: {}, 박스명: {}", 
                bucketList.getId(), account.getId(), finalBoxName);
        
        return account;
    }
    
    @Override
    @Transactional
    public Account createMoneyBoxForBucketList(BucketList bucketList, Member member, String boxName, 
                                               Boolean enableAutoTransfer, BigDecimal monthlyAmount, Integer transferDay) {
        // boxName이 없으면 버킷리스트 제목 사용
        String finalBoxName = (boxName != null && !boxName.trim().isEmpty()) ? boxName : bucketList.getTitle();
        
        // 머니박스 계좌 생성 (기본 비밀번호: 1234)
        Account account = createMoneyBoxAccount(member, finalBoxName, "1234");
        
        // BucketList와 Account 양방향 연결
        bucketList.setMoneyBoxAccount(account);
        
        // 자동이체 활성화된 경우 스케줄 생성 (AutoTransferScheduleService 사용)
        if (Boolean.TRUE.equals(enableAutoTransfer) && monthlyAmount != null && transferDay != null) {
            try {
                // 사용자의 주계좌 조회 (출금 계좌로 사용)
                Account mainAccount = findMainAccount(member)
                        .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
                
                // AutoTransferScheduleService를 통한 스케줄 생성 (다음달부터 시작)
                autoTransferScheduleService.createSchedule(mainAccount, account, monthlyAmount, transferDay);
                
                log.info("버킷리스트 머니박스 자동이체 스케줄 생성 완료 - 버킷리스트 ID: {}, 계좌 ID: {}, 월 납입금: {}, 이체일: {}일", 
                        bucketList.getId(), account.getId(), monthlyAmount, transferDay);
                        
            } catch (Exception e) {
                log.warn("자동이체 스케줄 생성 실패 (머니박스 생성은 완료됨) - 버킷리스트 ID: {}, error: {}", 
                        bucketList.getId(), e.getMessage());
                // 자동이체 스케줄 생성 실패해도 머니박스 생성은 성공으로 처리
            }
        }
        
        log.info("버킷리스트 연동 머니박스 생성 완료 - 버킷리스트 ID: {}, 계좌 ID: {}, 박스명: {}, 자동이체: {}", 
                bucketList.getId(), account.getId(), finalBoxName, enableAutoTransfer);
        
        return account;
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
    public MainAccountResponse getMainAccount(Member member) {
        Account mainAccount = findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        log.info("주계좌 조회 완료 - 회원 ID: {}, 계좌번호: {}", member.getId(), mainAccount.getNumber());
        
        return MainAccountResponse.of(mainAccount, member.isMainAccountLinked());
    }

    @Override
    public Long getMainAccountIdByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        Account mainAccount = findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        log.info("주계좌 ID 조회 완료 - 회원 ID: {}, 계좌 ID: {}", memberId, mainAccount.getId());
        
        return mainAccount.getId();
    }

    @Override
    public Account findMainAccountByMember(Member member) {
        Account mainAccount = findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        log.info("주계좌 조회 완료 - 회원 ID: {}, 계좌 ID: {}", member.getId(), mainAccount.getId());
        
        return mainAccount;
    }

    @Override
    public Account findById(Long accountId) {
        return accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    @Transactional
    public Account findByIdWithLock(Long accountId) {
        return accountRepository.findByIdAndDeletedFalseWithLock(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    public void validateAccountOwnership(Long accountId, Long memberId) {
        Account account = findById(accountId);
        if (!account.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
    }

    @Override
    public void validateAccountPassword(Long accountId, String password) {
        Account account = findById(accountId);
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }
    }

    @Override
    @Transactional
    public void debitBalance(Account account, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        account.updateBalance(newBalance);
        accountRepository.save(account);
        log.info("출금 처리 완료 - 계좌 ID: {}, 출금액: {}, 잔액: {}", account.getId(), amount, newBalance);
    }

    @Override
    @Transactional
    public void creditBalance(Account account, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance().add(amount);
        account.updateBalance(newBalance);
        accountRepository.save(account);
        log.info("입금 처리 완료 - 계좌 ID: {}, 입금액: {}, 잔액: {}", account.getId(), amount, newBalance);
    }
    
    @Override
    public long getMoneyBoxCountByMember(Member member) {
        long count = accountRepository.countByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
        log.info("머니박스 개수 조회 완료: memberId = {}, count = {}", member.getId(), count);
        return count;
    }
    
    @Override
    @Transactional
    public Account save(Account account) {
        Account savedAccount = accountRepository.save(account);
        log.info("계좌 저장 완료: accountId = {}", savedAccount.getId());
        return savedAccount;
    }

    @Override
    public Optional<Account> findMainAccount(Member member) {
        return accountRepository.findByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN);
    }

    @Override
    public List<Account> findMoneyBoxes(Member member) {
        return accountRepository.findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
    }

}