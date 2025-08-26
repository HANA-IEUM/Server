package com.hanaieum.server.domain.bucketList.dto;


import com.hanaieum.server.domain.bucketList.entity.BucketLIstType;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListResponse {

    private Long id; // 버킷리스트 ID
    private Long memberId; // 회원 ID
    private BucketLIstType type; // 카테고리
    private String title; // 제목
    private BigDecimal targetAmount; // 목표금액
    private LocalDate targetDate; // 목표기간

    private boolean publicFlag; // 공개여부
    private boolean togetherFlag; // 혼자/같이 여부

    private BucketListStatus status; // 상태
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시

    public static BucketListResponse from(BucketList bucketList) {
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
                .build();
    }
}
