package com.hanaieum.server.domain.support.dto;

import com.hanaieum.server.domain.support.entity.LetterColor;
import com.hanaieum.server.domain.support.entity.SupportRecord;
import com.hanaieum.server.domain.support.entity.SupportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "후원/응원 응답")
public class SupportResponse {

    @Schema(description = "후원/응원 ID", example = "1")
    private Long id;

    @Schema(description = "버킷리스트 ID", example = "10")
    private Long bucketListId;

    @Schema(description = "버킷리스트 제목", example = "유럽 여행가기")
    private String bucketListTitle;

    @Schema(description = "후원자/응원자 이름", example = "김하나")
    private String supporterName;

    @Schema(description = "지원 유형", example = "SPONSOR")
    private SupportType supportType;

    @Schema(description = "후원 금액 (응원시 0)", example = "50000")
    private BigDecimal supportAmount;

    @Schema(description = "응원 메시지", example = "화이팅! 꼭 이루시길 바라요!")
    private String message;

    @Schema(description = "편지지 색상", example = "PINK")
    private LetterColor letterColor;

    @Schema(description = "후원/응원 일시", example = "2024-12-28T15:30:00")
    private LocalDateTime supportedAt;

    public static SupportResponse of(SupportRecord supportRecord) {
        return SupportResponse.builder()
                .id(supportRecord.getId())
                .bucketListId(supportRecord.getBucketList().getId())
                .bucketListTitle(supportRecord.getBucketList().getTitle())
                .supporterName(supportRecord.getSupporter().getName())
                .supportType(supportRecord.getSupportType())
                .supportAmount(supportRecord.getSupportAmount())
                .message(supportRecord.getMessage())
                .letterColor(supportRecord.getLetterColor())
                .supportedAt(supportRecord.getCreatedAt())
                .build();
    }
}