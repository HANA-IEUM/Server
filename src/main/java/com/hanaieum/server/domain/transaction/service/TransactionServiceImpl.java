package com.hanaieum.server.domain.transaction.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import com.hanaieum.server.domain.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public Transaction createTransaction(Account fromAccount, Account toAccount, BigDecimal amount,
                                       TransactionType transactionType, ReferenceType referenceType,
                                       String description, Long referenceId) {
        Transaction transaction = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .transactionType(transactionType)
                .referenceType(referenceType)
                .description(description)
                .referenceId(referenceId)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        
        log.info("거래 기록 생성 완료 - ID: {}, 출금계좌: {}, 입금계좌: {}, 금액: {}, 타입: {}, 참조: {}", 
                savedTransaction.getId(), 
                fromAccount != null ? fromAccount.getId() : "없음", 
                toAccount != null ? toAccount.getId() : "없음",
                amount, transactionType, referenceType);
                
        return savedTransaction;
    }

}
