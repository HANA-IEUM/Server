package com.hanaieum.server.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyLivingCostUpdateRequest {
    @NotNull(message = "월 생활비는 필수입니다.")
    @Positive(message = "월 생활비는 양수여야 합니다.")
    private Integer monthlyLivingCost;
}