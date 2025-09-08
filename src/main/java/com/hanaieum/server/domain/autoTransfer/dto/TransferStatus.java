package com.hanaieum.server.domain.autoTransfer.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * 자동이체 현재/다음달 상태 정보 통합 DTO
 * MoneyBoxInfoResponse, MoneyBoxUpdateResponse 모두에서 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferStatus {
    
    // 현재 자동이체 상태
    private Boolean currentEnabled; // 현재 자동이체 활성화 여부
    private BigDecimal currentAmount; // 현재 월 납입금액
    private Integer currentTransferDay; // 현재 이체일
    
    // 다음달 자동이체 상태
    private Boolean nextEnabled; // 다음달 자동이체 활성화 여부
    private BigDecimal nextAmount; // 다음달 월 납입금액
    private Integer nextTransferDay; // 다음달 이체일
    
    /**
     * 자동이체가 완전히 비활성화된 상태
     */
    public static TransferStatus allDisabled() {
        return TransferStatus.builder()
                .currentEnabled(false)
                .currentAmount(null)
                .currentTransferDay(null)
                .nextEnabled(false)
                .nextAmount(null)
                .nextTransferDay(null)
                .build();
    }
    
    /**
     * 현재만 활성화된 상태 (다음달은 상황에 따라)
     */
    public static TransferStatus of(Boolean currentEnabled, BigDecimal currentAmount, Integer currentTransferDay,
                                   Boolean nextEnabled, BigDecimal nextAmount, Integer nextTransferDay) {
        return TransferStatus.builder()
                .currentEnabled(currentEnabled)
                .currentAmount(currentAmount)
                .currentTransferDay(currentTransferDay)
                .nextEnabled(nextEnabled)
                .nextAmount(nextAmount)
                .nextTransferDay(nextTransferDay)
                .build();
    }
    
    /**
     * MoneyBoxInfoResponse용 - 다음달 정보만 필요한 경우
     */
    public boolean isNextMonthEnabled() {
        return Boolean.TRUE.equals(nextEnabled);
    }
    
    /**
     * MoneyBoxUpdateResponse용 - 현재 상태 확인
     */
    public boolean isCurrentlyEnabled() {
        return Boolean.TRUE.equals(currentEnabled);
    }
}