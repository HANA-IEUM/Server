package com.hanaieum.server.domain.moneyBox.dto;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    
    public static MoneyBoxInfoResponse of(Account account, AutoTransferSchedule currentSchedule, AutoTransferSchedule futureSchedule) {
        // 다음달에 실제로 이체될 정보 계산 (validTo 고려)
        LocalDate nextMonth = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        Integer nextTransferDay = null;
        BigDecimal nextTransferAmount = null;
        
        if (futureSchedule != null) {
            // 미래 스케줄이 있으면 해당 스케줄이 다음달에 적용됨
            nextTransferDay = futureSchedule.getTransferDay();
            nextTransferAmount = futureSchedule.getAmount();
        } else if (currentSchedule != null) {
            // 현재 스케줄이 다음달에도 유효한지 확인
            boolean currentScheduleValidNextMonth = currentSchedule.getValidTo() == null || 
                    !currentSchedule.getValidTo().isBefore(nextMonth);
                    
            if (currentScheduleValidNextMonth) {
                // 현재 스케줄이 다음달에도 계속 적용됨
                nextTransferDay = currentSchedule.getTransferDay();
                nextTransferAmount = currentSchedule.getAmount();
            }
            // else: 현재 스케줄이 이번달로 종료 -> nextTransferDay, nextTransferAmount는 null 유지
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
                .nextTransferDay(nextTransferDay)
                .nextTransferAmount(nextTransferAmount)
                .bucketId(bucketId)
                .bucketTitle(bucketTitle)
                .build();
    }
}