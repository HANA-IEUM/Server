package com.hanaieum.server.domain.bucketList.dto;


import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListResponse {

    private Long id; // 버킷리스트 ID
    private Long memberId; // 회원 ID
    private BucketListType type; // 카테고리
    private String title; // 제목
    private BigDecimal targetAmount; // 목표금액
    private LocalDate targetDate; // 목표기간

    private boolean publicFlag; // 공개여부
    private boolean togetherFlag; // 혼자/같이 여부

    private BucketListStatus status; // 상태
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
    
    private List<BucketListParticipantDto> participants; // 참여자 목록 (같이 진행하는 경우)
    
    // 머니박스 정보 (기본 정보만)
    private MoneyBoxInfo moneyBoxInfo;

    public static BucketListResponse of(BucketList bucketList) {
        // 참여자 목록 생성 (활성화된 참여자만)
        List<BucketListParticipantDto> participants = bucketList.getParticipants().stream()
                .filter(participant -> participant.getActive())
                .map(BucketListParticipantDto::of)
                .toList();
        
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
        
        return BucketListResponse.builder()
                .id(bucketList.getId())
                .memberId(bucketList.getMember().getId())
                .type(bucketList.getType())
                .title(bucketList.getTitle())
                .targetAmount(bucketList.getTargetAmount())
                .targetDate(bucketList.getTargetDate())
                .publicFlag(bucketList.isPublicFlag())
                .togetherFlag(bucketList.isShareFlag())
                .status(bucketList.getStatus())
                .createdAt(bucketList.getCreatedAt())
                .updatedAt(bucketList.getUpdatedAt())
                .participants(participants)
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
