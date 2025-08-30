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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

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
                .counterpartyName(toAccount.getName())
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
                .counterpartyName(fromAccount.getName())
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        transactionRepository.save(depositTx);
        
        log.info("이체 거래 기록 생성 완료 - 출금: {}, 입금: {}, 금액: {}, 참조: {}", 
                fromAccount.getId(), toAccount.getId(), amount, referenceType);
    }

}
