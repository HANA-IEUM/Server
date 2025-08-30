package com.hanaieum.server.domain.moneyBox.dto;

import com.hanaieum.server.domain.account.entity.Account;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyBoxResponse {
    
    private Long accountId; // 머니박스 계좌 ID
    private String accountNumber; // 계좌번호
    private String accountName; // 계좌명
    private String bankName; // 은행명
    private BigDecimal balance; // 잔액
    private Long bucketListId; // 연결된 버킷리스트 ID
    private String bucketListTitle; // 버킷리스트 제목
    private BigDecimal targetAmount; // 버킷리스트 목표금액
    private String boxName; // 머니박스 별명
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
    
    public static MoneyBoxResponse of(Account account) {
        return MoneyBoxResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getNumber())
                .accountName(account.getName())
                .bankName(account.getBankName())
                .balance(account.getBalance())
                .bucketListId(account.getBucketList() != null ? account.getBucketList().getId() : null)
                .bucketListTitle(account.getBucketList() != null ? account.getBucketList().getTitle() : null)
                .targetAmount(account.getBucketList() != null ? account.getBucketList().getTargetAmount() : null)
                .boxName(account.getBoxName())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}