package com.hanaieum.server.domain.transaction.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;

import java.math.BigDecimal;

public interface TransactionService {
    
    Transaction createTransaction(Account fromAccount, Account toAccount, BigDecimal amount, 
                                TransactionType transactionType, ReferenceType referenceType, 
                                String description, Long referenceId);

}
