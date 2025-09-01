package com.hanaieum.server.domain.member.service;

import com.hanaieum.server.domain.member.dto.MemberMypageResponse;

public interface MemberService {
    
    void confirmMainAccountLink(Long memberId);
    
    void hideGroupPrompt(Long memberId);
    
    MemberMypageResponse getMypageInfo(Long memberId);
    
    MemberMypageResponse updateMonthlyLivingCost(Long memberId, Integer monthlyLivingCost);
}