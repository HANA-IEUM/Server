package com.hanaieum.server.domain.transaction.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;

import java.math.BigDecimal;

public interface TransactionService {
    
    void recordTransfer(Account fromAccount, Account toAccount, BigDecimal amount,
                       ReferenceType referenceType, String description, Long referenceId);

}
