package com.hanaieum.server.domain.moneyBox.dto;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.dto.TransferStatus;
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
    
    // 다음달 자동이체 예정 정보 (실제 다음달에 이체될 정보)
    private Integer nextTransferDay; // 다음달 자동이체일 (null이면 비활성화)
    private BigDecimal nextTransferAmount; // 다음달 자동이체 금액 (null이면 비활성화)
    
    private Long bucketId; // 연관된 버킷리스트 ID
    private String bucketTitle; // 버킷리스트 title
    
    public static MoneyBoxInfoResponse of(Account account, TransferStatus transferStatus) {
        
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
                .nextTransferDay(transferStatus.getNextTransferDay())
                .nextTransferAmount(transferStatus.getNextAmount())
                .bucketId(bucketId)
                .bucketTitle(bucketTitle)
                .build();
    }
}