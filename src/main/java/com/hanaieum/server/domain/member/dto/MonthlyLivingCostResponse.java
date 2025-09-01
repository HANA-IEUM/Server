package com.hanaieum.server.domain.member.dto;

import com.hanaieum.server.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyLivingCostResponse {
    private Integer monthlyLivingCost;

    public static MonthlyLivingCostResponse of(Member member) {
        return MonthlyLivingCostResponse.builder()
                .monthlyLivingCost(member.getMonthlyLivingCost())
                .build();
    }
}