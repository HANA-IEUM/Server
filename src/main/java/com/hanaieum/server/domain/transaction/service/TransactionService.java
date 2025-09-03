package com.hanaieum.server.domain.transaction.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.transaction.dto.TransactionResponse;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface TransactionService {
    
    void recordTransfer(Account fromAccount, Account toAccount, BigDecimal amount,
                       ReferenceType referenceType, String description, Long referenceId);

    // 이자 입금을 위한 메소드
    void recordDeposit(Account toAccount, BigDecimal amount, Long counterpartyAccountId,
                      String counterpartyName, ReferenceType referenceType, Long referenceId);
    
    Page<TransactionResponse> getTransactionsByAccountId(Long memberId, Long accountId, Pageable pageable);
}
