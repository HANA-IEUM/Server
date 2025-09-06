package com.hanaieum.server.domain.transfer.service;

import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import java.math.BigDecimal;

public interface TransferService {
    
    void fillMoneyBox(Long memberId, Long moneyBoxAccountId, BigDecimal amount, String password);
    
    void sponsorBucket(Long sponsorMemberId, Long bucketId, BigDecimal amount, String password);

    void executeAutoTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long scheduleId);

    BigDecimal withdrawAllFromMoneyBox(Long memberId, Long moneyBoxAccountId, Long referenceId);

    void payInterest(Long memberId, BigDecimal interestAmount, Long bucketListId);
}