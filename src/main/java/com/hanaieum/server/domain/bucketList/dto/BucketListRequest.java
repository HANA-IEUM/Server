package com.hanaieum.server.domain.bucketList.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketLIstType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListRequest {
    
    @NotNull(message = "카테고리는 필수 입력값입니다.")
    private BucketLIstType type; // 카테고리
    
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title; // 제목
    
    @NotNull(message = "목표 금액은 필수 입력값입니다.")
    @Positive(message = "목표 금액은 0보다 커야 합니다.")
    private BigDecimal targetAmount; // 목표금액
    
    @NotNull(message = "목표 날짜는 필수 입력값입니다.")
    private LocalDate targetDate; // 목표기간
    
    @NotNull(message = "공개 여부는 필수 입력값입니다.")
    private Boolean isPublic; // 공개여부
    
    @NotNull(message = "혼자/함께 여부는 필수 입력값입니다.")
    private Boolean isTogether; // 혼자/같이 여부
}
