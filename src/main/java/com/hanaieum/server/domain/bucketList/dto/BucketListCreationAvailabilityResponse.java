package com.hanaieum.server.domain.bucketList.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "버킷리스트 생성 가능 여부 응답")
@Getter
@AllArgsConstructor
public class BucketListCreationAvailabilityResponse {
    
    @Schema(description = "생성 가능 여부", example = "true")
    private boolean canCreate;
    
    @Schema(description = "현재 머니박스 개수", example = "5")
    private long currentMoneyBoxCount;
}