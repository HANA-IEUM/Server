package com.hanaieum.server.domain.transfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
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

    @Override
    public void fillMoneyBox(Long fromAccountId, Long toAccountId, BigDecimal amount, String password) {
        log.info("머니박스 채우기 시작 - 출금계좌: {}, 입금계좌: {}, 금액: {}", fromAccountId, toAccountId, amount);
        
        // 1. 계좌 소유권 검증은 컨트롤러에서 수행되었다고 가정
        
        // 2. 출금 계좌 비밀번호 검증
        accountService.validateAccountPassword(fromAccountId, password);
        
        // 3. 잔액 검증
        accountService.validateSufficientBalance(fromAccountId, amount);
        
        // 4. 계좌 정보 조회 (락과 함께) - deleted=false 조건 자동 적용
        Account fromAccount = accountService.findByIdWithLock(fromAccountId);
        Account toAccount = accountService.findByIdWithLock(toAccountId);
        
        // 5. 출금 처리
        accountService.debitBalance(fromAccountId, amount);
        
        // 6. 입금 처리  
        accountService.creditBalance(toAccountId, amount);
        
        // 7. 거래 내역 기록
        transactionService.createTransaction(
            fromAccount, 
            toAccount, 
            amount, 
            TransactionType.TRANSFER,
            ReferenceType.MONEY_BOX_TRANSFER,
            ReferenceType.MONEY_BOX_TRANSFER.getDescription(),
            null
        );
        
        log.info("머니박스 채우기 완료 - 출금계좌: {}, 입금계좌: {}, 금액: {}", fromAccountId, toAccountId, amount);
    }

    @Override
    public void sponsorMoneyBox(Long fromAccountId, Long toAccountId, BigDecimal amount, String password, Long bucketId) {
        log.info("머니박스 후원 시작 - 출금계좌: {}, 입금계좌: {}, 금액: {}, 버킷ID: {}", fromAccountId, toAccountId, amount, bucketId);
        
        // 1. 계좌 소유권 검증은 컨트롤러에서 수행되었다고 가정
        
        // 2. 출금 계좌 비밀번호 검증
        accountService.validateAccountPassword(fromAccountId, password);
        
        // 3. 잔액 검증
        accountService.validateSufficientBalance(fromAccountId, amount);
        
        // 4. 계좌 정보 조회 (락과 함께) - deleted=false 조건 자동 적용
        Account fromAccount = accountService.findByIdWithLock(fromAccountId);
        Account toAccount = accountService.findByIdWithLock(toAccountId);
        
        // 5. 출금 처리
        accountService.debitBalance(fromAccountId, amount);
        
        // 6. 입금 처리
        accountService.creditBalance(toAccountId, amount);
        
        // 7. 거래 내역 기록
        transactionService.createTransaction(
            fromAccount, 
            toAccount, 
            amount, 
            TransactionType.TRANSFER,
            ReferenceType.BUCKET_FUNDING,
            ReferenceType.BUCKET_FUNDING.getDescription(),
            bucketId
        );
        
        log.info("머니박스 후원 완료 - 출금계좌: {}, 입금계좌: {}, 금액: {}, 버킷ID: {}", fromAccountId, toAccountId, amount, bucketId);
    }
}