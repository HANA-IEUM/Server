package com.hanaieum.server.domain.member.service;

public interface MemberService {
    
    void confirmMainAccountLink(Long memberId);
    
    void hideGroupPrompt(Long memberId);
}