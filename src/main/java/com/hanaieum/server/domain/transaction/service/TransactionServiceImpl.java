package com.hanaieum.server.domain.transaction.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.transaction.dto.TransactionResponse;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import com.hanaieum.server.domain.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Override
    public void recordTransfer(Account fromAccount, Account toAccount, BigDecimal amount,
                               ReferenceType referenceType, String description, Long referenceId) {

        // 출금 레코드 생성
        Transaction withdrawTx = Transaction.builder()
                .account(fromAccount)
                .transactionType(TransactionType.WITHDRAW)
                .amount(amount)
                .balanceAfter(fromAccount.getBalance()) // debit 이후 값
                .counterpartyAccountId(toAccount.getId())
                .counterpartyName(toAccount.getMember().getName())
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        transactionRepository.save(withdrawTx);

        // 입금 레코드 생성
        Transaction depositTx = Transaction.builder()
                .account(toAccount)
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(toAccount.getBalance()) // credit 이후 값
                .counterpartyAccountId(fromAccount.getId())
                .counterpartyName(fromAccount.getMember().getName())
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        transactionRepository.save(depositTx);
        
        log.info("이체 거래 기록 생성 완료 - 출금: {}, 입금: {}, 금액: {}, 참조: {}", 
                fromAccount.getId(), toAccount.getId(), amount, referenceType);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByAccountId(Long memberId, Long accountId, Pageable pageable) {

        // 계좌 소유권 검증
        accountService.validateAccountOwnership(accountId, memberId);
        
        Page<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        
        log.info("거래내역 조회 완료 - 회원 ID: {}, 계좌 ID: {}, 페이지: {}, 사이즈: {}, 총 개수: {}", 
                memberId, accountId, pageable.getPageNumber(), pageable.getPageSize(), transactions.getTotalElements());
        
        return transactions.map(TransactionResponse::of);
    }
}
