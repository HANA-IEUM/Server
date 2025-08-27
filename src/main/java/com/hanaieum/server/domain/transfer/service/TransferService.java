package com.hanaieum.server.domain.transfer.service;

import java.math.BigDecimal;

public interface TransferService {
    
    void fillMoneyBox(Long fromAccountId, Long toAccountId, BigDecimal amount, String password);
    
    void sponsorMoneyBox(Long fromAccountId, Long toAccountId, BigDecimal amount, String password, Long bucketId);
    
}