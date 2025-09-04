package com.hanaieum.server.domain.transfer.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
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
        
        // 1. 회원의 주계좌 ID 조회
        Long fromAccountId = accountService.getMainAccountIdByMemberId(memberId);
        
        // 2. 동일한 계좌로 이체하는지 체크
        if (fromAccountId.equals(moneyBoxAccountId)) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER_SAME_ACCOUNT);
        }
        
        // 3. 머니박스 계좌 소유권 검증
        accountService.validateAccountOwnership(moneyBoxAccountId, memberId);
        
        // 4. 비밀번호 검증
        accountService.validateAccountPassword(fromAccountId, password);
        
        // 5. 이체 실행
        executeTransfer(fromAccountId, moneyBoxAccountId, amount, ReferenceType.MONEY_BOX_DEPOSIT, null);
        
        log.info("머니박스 채우기 완료 - 회원 ID: {}, 머니박스: {}, 금액: {}", memberId, moneyBoxAccountId, amount);
    }

    @Override
    public void sponsorBucket(Long sponsorMemberId, Long bucketId, BigDecimal amount, String password) {
        log.info("버킷 후원 시작 - 후원자 ID: {}, 버킷 ID: {}, 금액: {}", sponsorMemberId, bucketId, amount);
        
        // 1. 후원자의 주계좌 ID 조회
        Long fromAccountId = accountService.getMainAccountIdByMemberId(sponsorMemberId);
        
        // 2. 버킷리스트 ID로 머니박스 계좌 조회
        Long moneyBoxAccountId = getMoneyBoxAccountIdByBucketId(bucketId);
        
        // 3. 비밀번호 검증
        accountService.validateAccountPassword(fromAccountId, password);
        
        // 4. 이체 실행
        executeTransfer(fromAccountId, moneyBoxAccountId, amount, ReferenceType.BUCKET_FUNDING, bucketId);
        
        log.info("버킷 후원 완료 - 후원자 ID: {}, 버킷 ID: {}, 금액: {}", sponsorMemberId, bucketId, amount);
    }

    @Override
    public BigDecimal withdrawAllFromMoneyBox(Long memberId, Long moneyBoxAccountId, ReferenceType referenceType, Long referenceId) {
        log.info("머니박스 전액 인출 시작 - 회원 ID: {}, 머니박스: {}, 참조: {}", memberId, moneyBoxAccountId, referenceType);

        // 1. 회원의 주계좌 ID 조회
        Long mainAccountId = accountService.getMainAccountIdByMemberId(memberId);
        
        // 2. 머니박스 계좌 조회 및 잔액 확인
        Account moneyBoxAccount = accountService.findByIdWithLock(moneyBoxAccountId);
        BigDecimal balance = moneyBoxAccount.getBalance();
        
        // 3. 잔액이 0보다 클 때만 이체 실행
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            executeTransfer(moneyBoxAccountId, mainAccountId, balance, referenceType, referenceId);
            log.info("머니박스 전액 인출 완료 - 회원 ID: {}, 머니박스: {} → 주계좌: {}, 인출금액: {}", 
                    memberId, moneyBoxAccountId, mainAccountId, balance);
            return balance;
        } else {
            log.info("머니박스 전액 인출 완료 - 회원 ID: {}, 머니박스: {}, 잔액이 0이므로 이체하지 않음", 
                    memberId, moneyBoxAccountId);
            return BigDecimal.ZERO;
        }
    }

    private void executeTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, 
                               ReferenceType referenceType, Long referenceId) {
        // 1. 계좌 조회 (락 걸기)
        Account fromAccount = accountService.findByIdWithLock(fromAccountId);
        Account toAccount = accountService.findByIdWithLock(toAccountId);
        
        // 2. 출금 (잔액 검증도 처리)
        accountService.debitBalance(fromAccount.getId(), amount);
        
        // 3. 입금
        accountService.creditBalance(toAccount.getId(), amount);
        
        // 4. 거래내역 2건 생성
        transactionService.recordTransfer(
            fromAccount,
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

    @Override
    public void payInterest(Long memberId, BigDecimal interestAmount, Long bucketListId) {
        log.info("이자 지급 시작 - 회원 ID: {}, 이자: {}, 버킷리스트 ID: {}", memberId, interestAmount, bucketListId);
        
        // 1. 회원의 주계좌 조회
        Long mainAccountId = accountService.getMainAccountIdByMemberId(memberId);
        Account mainAccount = accountService.findByIdWithLock(mainAccountId);
        
        // 2. 이자 거래 기록 생성 (상대방: 하나이음)
        transactionService.recordDeposit(
                mainAccount, 
                interestAmount,
                null, // 상대방 계좌 없음 (은행에서 지급)
                "하나이음", // 상대방 이름
                ReferenceType.MONEY_BOX_INTEREST,
                ReferenceType.MONEY_BOX_INTEREST.getDescription(),
                bucketListId
        );
        
        // 3. 실제 주계좌 잔액에 이자 추가
        accountService.creditBalance(mainAccount.getId(), interestAmount);
        
        log.info("목표 달성 이자 지급 완료: 주계좌 {}, 이자: {}", mainAccount.getId(), interestAmount);
    }
}