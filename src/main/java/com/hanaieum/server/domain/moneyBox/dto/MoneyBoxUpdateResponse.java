package com.hanaieum.server.domain.moneyBox.dto;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
    
    public static MoneyBoxUpdateResponse of(Account account, 
                                          Optional<AutoTransferSchedule> currentSchedule,
                                          Optional<AutoTransferSchedule> futureSchedule) {
        
        // 다음달에 실제로 적용될 자동이체 정보 계산
        LocalDate nextMonth = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        boolean nextAutoTransferEnabled;
        BigDecimal nextMonthlyAmount = null;
        Integer nextTransferDay = null;
        
        if (futureSchedule.isPresent()) {
            // 미래 스케줄이 있으면 해당 스케줄이 다음달에 적용됨
            AutoTransferSchedule nextSchedule = futureSchedule.get();
            nextAutoTransferEnabled = true;
            nextMonthlyAmount = nextSchedule.getAmount();
            nextTransferDay = nextSchedule.getTransferDay();
        } else if (currentSchedule.isPresent()) {
            // 현재 스케줄이 있는 경우 validTo 확인
            AutoTransferSchedule current = currentSchedule.get();
            
            // 현재 스케줄이 다음달에도 유효한지 확인 (validTo가 null이거나 다음달까지 유효)
            boolean currentScheduleValidNextMonth = current.getValidTo() == null || 
                    !current.getValidTo().isBefore(nextMonth);
            
            if (currentScheduleValidNextMonth) {
                // 현재 스케줄이 다음달에도 계속 적용됨
                nextAutoTransferEnabled = true;
                nextMonthlyAmount = current.getAmount();
                nextTransferDay = current.getTransferDay();
            } else {
                // 현재 스케줄이 이번달로 종료됨 -> 다음달은 비활성화
                nextAutoTransferEnabled = false;
            }
        } else {
            // 현재도 미래도 스케줄이 없으면 다음달에도 비활성화
            nextAutoTransferEnabled = false;
        }
        
        return MoneyBoxUpdateResponse.builder()
                .boxId(account.getId())
                .boxName(account.getBoxName())
                .autoTransferEnabled(currentSchedule.isPresent())
                .currentMonthlyAmount(currentSchedule.map(AutoTransferSchedule::getAmount).orElse(null))
                .currentTransferDay(currentSchedule.map(AutoTransferSchedule::getTransferDay).orElse(null))
                .nextAutoTransferEnabled(nextAutoTransferEnabled)
                .nextMonthlyAmount(nextMonthlyAmount)
                .nextTransferDay(nextTransferDay)
                .build();
    }
}