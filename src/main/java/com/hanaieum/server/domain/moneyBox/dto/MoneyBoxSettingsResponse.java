package com.hanaieum.server.domain.moneyBox.dto;

import com.hanaieum.server.domain.moneyBox.entity.MoneyBoxSettings;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyBoxSettingsResponse {
    
    private Long id; // 머니박스 설정 ID
    private Long accountId; // 머니박스 계좌 ID
    private String accountNumber; // 계좌번호
    private String accountName; // 계좌명
    private String bankName; // 은행명
    private Long balance; // 잔액
    private Long bucketListId; // 연결된 버킷리스트 ID
    private String bucketListTitle; // 버킷리스트 제목
    private BigDecimal targetAmount; // 버킷리스트 목표금액
    private String boxName; // 머니박스 별명
    
    // 자동이체 설정
    private BigDecimal monthlyPaymentAmount; // 월 납입 금액
    private Boolean autoTransferEnabled; // 자동이체 활성화 여부
    private Integer autoTransferDay; // 자동이체 날짜
    private String sourceAccountNumber; // 출금 계좌번호
    
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
    
    public static MoneyBoxSettingsResponse of(MoneyBoxSettings settings) {
        return MoneyBoxSettingsResponse.builder()
                .id(settings.getId())
                .accountId(settings.getAccount().getId())
                .accountNumber(settings.getAccount().getNumber())
                .accountName(settings.getAccount().getName())
                .bankName(settings.getAccount().getBankName())
                .balance(settings.getAccount().getBalance())
                .bucketListId(settings.getBucketList().getId())
                .bucketListTitle(settings.getBucketList().getTitle())
                .targetAmount(settings.getBucketList().getTargetAmount())
                .boxName(settings.getBoxName())
                .monthlyPaymentAmount(settings.getMonthlyPaymentAmount())
                .autoTransferEnabled(settings.getAutoTransferEnabled())
                .autoTransferDay(settings.getAutoTransferDay())
                .sourceAccountNumber(settings.getSourceAccountNumber())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
