package com.hanaieum.server.domain.member.service;

import com.hanaieum.server.domain.member.dto.MemberMypageResponse;
import com.hanaieum.server.domain.member.dto.MonthlyLivingCostResponse;

public interface MemberService {
    
    void confirmMainAccountLink(Long memberId);
    
    void hideGroupPrompt(Long memberId);
    
    MemberMypageResponse getMypageInfo(Long memberId);

    MonthlyLivingCostResponse getMonthlyLivingCost(Long memberId);

    MemberMypageResponse updateMonthlyLivingCost(Long memberId, Integer monthlyLivingCost);
}