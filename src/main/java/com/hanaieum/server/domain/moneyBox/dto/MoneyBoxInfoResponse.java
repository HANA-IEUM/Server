package com.hanaieum.server.domain.moneyBox.dto;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyBoxInfoResponse {
    
    private Long boxId; // 계좌 ID
    private String boxName; // 박스이름
    private BigDecimal balance; // 잔액
    private Integer autoTransferDay; // 자동이체일 (null 가능)
    private BigDecimal autoTransferAmount; // 자동이체 금액 (null 가능)
    private Long bucketId; // 연관된 버킷리스트 ID
    private String bucketTitle; // 버킷리스트 title
    
    public static MoneyBoxInfoResponse of(Account account, AutoTransferSchedule autoTransfer) {
        // 자동이체 정보
        Integer transferDay = null;
        BigDecimal transferAmount = null;
        if (autoTransfer != null) {
            transferDay = autoTransfer.getTransferDay();
            transferAmount = autoTransfer.getAmount();
        }
        
        // 버킷리스트 정보
        Long bucketId = null;
        String bucketTitle = null;
        if (account.getBucketList() != null) {
            bucketId = account.getBucketList().getId();
            bucketTitle = account.getBucketList().getTitle();
        }
        
        return MoneyBoxInfoResponse.builder()
                .boxId(account.getId())
                .boxName(account.getBoxName())
                .balance(account.getBalance())
                .autoTransferDay(transferDay)
                .autoTransferAmount(transferAmount)
                .bucketId(bucketId)
                .bucketTitle(bucketTitle)
                .build();
    }
}