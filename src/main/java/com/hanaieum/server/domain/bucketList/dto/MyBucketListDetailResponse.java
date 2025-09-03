package com.hanaieum.server.domain.bucketList.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyBucketListDetailResponse {

    private String title; // 버킷리스트 이름
    private BigDecimal targetAmount; // 목표금액
    private LocalDate targetDate; // 목표기간 종료날짜
    private boolean togetherFlag; // 혼자/같이 여부
    private BucketListStatus bucketListStatus; // 상태
    private boolean canComplete; // 달성 버튼 활성화 여부
    private MoneyBoxInfo moneyBoxInfo; // 머니박스 정보
    private List<BucketListParticipantDto> participants; // 참여자 목록 (같이 진행하는 경우)

    public static MyBucketListDetailResponse of(BucketList bucketList) {
        // 참여자 목록 생성 (활성화된 참여자만)
        List<BucketListParticipantDto> participants = bucketList.getParticipants().stream()
                .filter(participant -> participant.getActive())
                .map(BucketListParticipantDto::of)
                .toList();

        // 머니박스 정보 생성 (모든 버킷리스트에 머니박스가 존재)
        MoneyBoxInfo moneyBoxInfo = MoneyBoxInfo.builder()
                .accountId(bucketList.getMoneyBoxAccount().getId())
                .boxName(bucketList.getMoneyBoxAccount().getBoxName())
                .accountNumber(bucketList.getMoneyBoxAccount().getNumber())
                .balance(bucketList.getMoneyBoxAccount().getBalance())
                .build();

        // canComplete 계산: COMPLETED이면 false, IN_PROGRESS이면서 오늘날짜가 targetDate를 지났거나 같으면 true
        boolean canComplete = bucketList.getStatus() == com.hanaieum.server.domain.bucketList.entity.BucketListStatus.IN_PROGRESS
                && (LocalDate.now().isEqual(bucketList.getTargetDate()) || LocalDate.now().isAfter(bucketList.getTargetDate()));

        return MyBucketListDetailResponse.builder()
                .title(bucketList.getTitle())
                .targetAmount(bucketList.getTargetAmount())
                .targetDate(bucketList.getTargetDate())
                .togetherFlag(bucketList.isShareFlag())
                .bucketListStatus(bucketList.getStatus())
                .canComplete(canComplete)
                .participants(participants)
                .moneyBoxInfo(moneyBoxInfo)
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoneyBoxInfo {
        private Long accountId; // Account ID
        private String boxName; // 머니박스 이름
        private String accountNumber; // 계좌번호
        private BigDecimal balance; // 잔액
    }
}
