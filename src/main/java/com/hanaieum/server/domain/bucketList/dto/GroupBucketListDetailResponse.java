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
public class GroupBucketListDetailResponse {

    private String title; // 버킷리스트 이름
    private BigDecimal targetAmount; // 목표금액
    private LocalDate targetDate; // 목표기간 종료날짜
    private boolean togetherFlag; // 혼자/같이 여부
    private BucketListStatus bucketListStatus; // 상태
    private List<BucketListParticipantDto> participants; // 참여자 목록 (같이 진행하는 경우)

    public static GroupBucketListDetailResponse of(BucketList bucketList) {
        // 참여자 목록 생성 (활성화된 참여자만)
        List<BucketListParticipantDto> participants = bucketList.getParticipants().stream()
                .filter(participant -> participant.getActive())
                .map(BucketListParticipantDto::of)
                .toList();

        return GroupBucketListDetailResponse.builder()
                .title(bucketList.getTitle())
                .targetAmount(bucketList.getTargetAmount())
                .targetDate(bucketList.getTargetDate())
                .togetherFlag(bucketList.isShareFlag())
                .bucketListStatus(bucketList.getStatus())
                .participants(participants)
                .build();
    }

}
