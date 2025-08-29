package com.hanaieum.server.domain.bucketList.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketParticipant;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListParticipantDto {
    
    private Long memberId; // 참여자 회원 ID
    private String memberName; // 참여자 이름
    private LocalDateTime joinedAt; // 참여일시
    private Boolean isActive; // 활성 여부
    
    public static BucketListParticipantDto of(BucketParticipant participant) {
        return BucketListParticipantDto.builder()
                .memberId(participant.getMember().getId())
                .memberName(participant.getMember().getName())
                .joinedAt(participant.getJoinedAt())
                .isActive(participant.getIsActive())
                .build();
    }
}
