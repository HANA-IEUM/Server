package com.hanaieum.server.domain.bucketList.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListUpdateRequest {
    
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title; // 제목
}
