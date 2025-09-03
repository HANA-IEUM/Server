package com.hanaieum.server.domain.transfer.service;

import com.hanaieum.server.domain.transaction.entity.ReferenceType;
import java.math.BigDecimal;

public interface TransferService {
    
    void fillMoneyBox(Long memberId, Long moneyBoxAccountId, BigDecimal amount, String password);
    
    void sponsorBucket(Long sponsorMemberId, Long bucketId, BigDecimal amount, String password);

    /**
     * 계좌 간 직접 이체 (내부 시스템용)
     * @param fromAccountId 출금 계좌 ID
     * @param toAccountId 입금 계좌 ID
     * @param amount 이체 금액
     * @param referenceType 거래 참조 유형 (description은 내부에서 getDescription() 사용)
     * @param referenceId 참조 ID (버킷리스트 ID 등)
     */
    void transferBetweenAccounts(Long fromAccountId, Long toAccountId, BigDecimal amount,
                               ReferenceType referenceType, Long referenceId);
    
}