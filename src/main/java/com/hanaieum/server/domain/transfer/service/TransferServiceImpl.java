package com.hanaieum.server.domain.transfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransferServiceImpl implements TransferService {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final BucketListRepository bucketListRepository;

    @Override
    public void fillMoneyBox(Long memberId, Long moneyBoxAccountId, BigDecimal amount, String password) {
        log.info("머니박스 채우기 시작 - 회원 ID: {}, 머니박스: {}, 금액: {}", memberId, moneyBoxAccountId, amount);
        
        // 1. 회원의 주계좌 조회
        Account fromAccount = accountService.getMainAccountByMemberId(memberId);
        
        // 2. 머니박스 계좌 소유권 검증
        accountService.validateAccountOwnership(moneyBoxAccountId, memberId);
        
        // 3. 이체 실행
        executeTransfer(fromAccount, moneyBoxAccountId, amount, password, 
                      ReferenceType.MONEY_BOX_TRANSFER, null);
        
        log.info("머니박스 채우기 완료 - 회원 ID: {}, 머니박스: {}, 금액: {}", memberId, moneyBoxAccountId, amount);
    }

    @Override
    public void sponsorBucket(Long sponsorMemberId, Long bucketId, BigDecimal amount, String password) {
        log.info("버킷 후원 시작 - 후원자 ID: {}, 버킷 ID: {}, 금액: {}", sponsorMemberId, bucketId, amount);
        
        // 1. 후원자의 주계좌 조회
        Account fromAccount = accountService.getMainAccountByMemberId(sponsorMemberId);
        
        // 2. 버킷리스트 ID로 머니박스 계좌 조회
        Long moneyBoxAccountId = getMoneyBoxAccountIdByBucketId(bucketId);
        
        // 3. 이체 실행 (후원은 소유권 검증 없음)
        executeTransfer(fromAccount, moneyBoxAccountId, amount, password, 
                      ReferenceType.BUCKET_FUNDING, bucketId);
        
        log.info("버킷 후원 완료 - 후원자 ID: {}, 버킷 ID: {}, 금액: {}", sponsorMemberId, bucketId, amount);
    }

    @Override
    public void achieveBucket(Long memberId, Long bucketId, BigDecimal amount, String password) {
        log.info("버킷 달성 인출 시작 - 회원 ID: {}, 버킷 ID: {}, 금액: {}", memberId, bucketId, amount);
        
        // 1. 회원의 주계좌 조회
        Account toAccount = accountService.getMainAccountByMemberId(memberId);
        
        // 2. 버킷리스트 ID로 머니박스 계좌 조회 및 소유권 검증
        Long moneyBoxAccountId = getMoneyBoxAccountIdByBucketId(bucketId);
        accountService.validateAccountOwnership(moneyBoxAccountId, memberId);
        
        // 3. 이체 실행 (머니박스 → 주계좌)
        executeTransfer(accountService.findById(moneyBoxAccountId), toAccount.getId(), 
                      amount, password, ReferenceType.BUCKET_ACHIEVEMENT, bucketId);
        
        log.info("버킷 달성 인출 완료 - 회원 ID: {}, 버킷 ID: {}, 금액: {}", memberId, bucketId, amount);
    }

    private void executeTransfer(Account fromAccount, Long toAccountId, BigDecimal amount, 
                               String password, ReferenceType referenceType, Long referenceId) {
        // 1. 비밀번호 검증
        accountService.validateAccountPassword(fromAccount.getId(), password);
        
        // 2. 잔액 검증
        accountService.validateSufficientBalance(fromAccount.getId(), amount);
        
        // 3. 계좌 조회 (락 걸기)
        Account lockedFromAccount = accountService.findByIdWithLock(fromAccount.getId());
        Account toAccount = accountService.findByIdWithLock(toAccountId);
        
        // 4. 출금
        accountService.debitBalance(lockedFromAccount.getId(), amount);
        
        // 5. 입금
        accountService.creditBalance(toAccount.getId(), amount);
        
        // 6. 거래내역 2건 생성
        transactionService.recordTransfer(
            lockedFromAccount,
            toAccount,
            amount,
            referenceType,
            referenceType.getDescription(),
            referenceId
        );
    }

    private Long getMoneyBoxAccountIdByBucketId(Long bucketId) {
        // 1. 버킷리스트 조회 -> 머니박스 계좌 ID 조회
        BucketList bucketList = bucketListRepository.findByIdAndDeletedFalse(bucketId)
                .orElseThrow(() -> new RuntimeException("버킷리스트를 찾을 수 없습니다: " + bucketId));
        
        // 2. 버킷리스트에 연결된 머니박스 계좌 ID 반환
        if (bucketList.getMoneyBoxAccount() == null) {
            throw new RuntimeException("버킷리스트에 연결된 머니박스가 없습니다: " + bucketId);
        }
        
        Long accountId = bucketList.getMoneyBoxAccount().getId();
        log.info("버킷리스트 {} → 머니박스 계좌 {} 매핑 완료", bucketId, accountId);
        
        return accountId;
    }
}