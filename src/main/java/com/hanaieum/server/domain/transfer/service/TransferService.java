package com.hanaieum.server.domain.transfer.service;

import java.math.BigDecimal;

public interface TransferService {
    
    void fillMoneyBox(Long memberId, Long moneyBoxAccountId, BigDecimal amount, String password);
    
    void sponsorBucket(Long sponsorMemberId, Long bucketId, BigDecimal amount, String password);
    
    void achieveBucket(Long memberId, Long bucketId, BigDecimal amount, String password);
    
}