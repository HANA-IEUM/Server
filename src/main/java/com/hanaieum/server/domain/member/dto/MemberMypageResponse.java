package com.hanaieum.server.domain.member.dto;

import com.hanaieum.server.domain.member.entity.Member;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberMypageResponse {
    private String name;
    private String phoneNumber;
    private Integer monthlyLivingCost;

    public static MemberMypageResponse of(Member member) {
        return MemberMypageResponse.builder()
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .monthlyLivingCost(member.getMonthlyLivingCost())
                .build();
    }
}