package com.hanaieum.server.domain.bucketList.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListDetailResponse {

    private String title; // 버킷리스트 이름
    private BigDecimal targetAmount; // 목표금액
    private LocalDate targetDate; // 목표기간 종료날짜
    private MoneyBoxInfo moneyBoxInfo; // 머니박스 정보

    public static BucketListDetailResponse of(BucketList bucketList) {
        // 머니박스 정보 생성
        MoneyBoxInfo moneyBoxInfo = null;
        if (bucketList.getMoneyBoxAccount() != null) {
            moneyBoxInfo = MoneyBoxInfo.builder()
                    .accountId(bucketList.getMoneyBoxAccount().getId())
                    .boxName(bucketList.getMoneyBoxAccount().getBoxName())
                    .accountNumber(bucketList.getMoneyBoxAccount().getNumber())
                    .balance(bucketList.getMoneyBoxAccount().getBalance())
                    .hasMoneyBox(true)
                    .build();
        } else {
            moneyBoxInfo = MoneyBoxInfo.builder()
                    .hasMoneyBox(false)
                    .build();
        }

        return BucketListDetailResponse.builder()
                .title(bucketList.getTitle())
                .targetAmount(bucketList.getTargetAmount())
                .targetDate(bucketList.getTargetDate())
                .moneyBoxInfo(moneyBoxInfo)
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoneyBoxInfo {
        private Long accountId; // Account ID  
        private String boxName; // 머니박스 이름
        private String accountNumber; // 계좌번호
        private BigDecimal balance; // 잔액
        private boolean hasMoneyBox; // 머니박스 존재 여부
    }
}
