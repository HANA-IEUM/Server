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
public class MoneyBoxUpdateResponse {
    
    private Long boxId; // 계좌 ID
    private String boxName; // 머니박스 별명
    
    // 자동이체 현재 상태
    private Boolean autoTransferEnabled; // 현재 자동이체 활성화 여부
    private BigDecimal currentMonthlyAmount; // 현재 월 납입금액
    private Integer currentTransferDay; // 현재 이체일
    
    // 다음달부터 적용될 설정 (변경이 있는 경우)
    private Boolean nextAutoTransferEnabled; // 다음달부터 적용될 자동이체 활성화 여부
    private BigDecimal nextMonthlyAmount; // 다음달 월 납입금액
    private Integer nextTransferDay; // 다음달 이체일
    
    public static MoneyBoxUpdateResponse of(Account account, TransferStatus transferStatus) {
        
        return MoneyBoxUpdateResponse.builder()
                .boxId(account.getId())
                .boxName(account.getBoxName())
                .autoTransferEnabled(transferStatus.getCurrentEnabled())
                .currentMonthlyAmount(transferStatus.getCurrentAmount())
                .currentTransferDay(transferStatus.getCurrentTransferDay())
                .nextAutoTransferEnabled(transferStatus.getNextEnabled())
                .nextMonthlyAmount(transferStatus.getNextAmount())
                .nextTransferDay(transferStatus.getNextTransferDay())
                .build();
    }
}