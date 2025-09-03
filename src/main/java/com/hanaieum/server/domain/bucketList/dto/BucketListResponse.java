package com.hanaieum.server.domain.bucketList.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import lombok.*;

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

    private boolean publicFlag; // 공개여부
    private boolean togetherFlag; // 혼자/같이 여부
    private LocalDate targetDate; // 목표기간 종료날짜

    private BucketListStatus status; // 상태
    private LocalDateTime createdAt; // 생성일시

    private List<BucketListParticipantDto> participants; // 참여자 목록 (같이 진행하는 경우)

    public static BucketListResponse of(BucketList bucketList) {
        // 참여자 목록 생성 (활성화된 참여자만)
        List<BucketListParticipantDto> participants = bucketList.getParticipants().stream()
                .filter(participant -> participant.getActive())
                .map(BucketListParticipantDto::of)
                .toList();

        return BucketListResponse.builder()
                .id(bucketList.getId())
                .memberId(bucketList.getMember().getId())
                .type(bucketList.getType())
                .title(bucketList.getTitle())
                .publicFlag(bucketList.isPublicFlag())
                .togetherFlag(bucketList.isShareFlag())
                .targetDate(bucketList.getTargetDate())
                .status(bucketList.getStatus())
                .createdAt(bucketList.getCreatedAt())
                .participants(participants)
                .build();
    }
}