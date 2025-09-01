package com.hanaieum.server.domain.member.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.member.dto.MemberMypageResponse;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public void confirmMainAccountLink(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        member.setMainAccountLinked(true);
        memberRepository.save(member);

        log.info("주계좌 연결 확인 완료 - 회원 ID: {}", memberId);
    }

    @Override
    public void hideGroupPrompt(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        member.setHideGroupPrompt(true);
        memberRepository.save(member);

        log.info("그룹 안내 숨김 처리 완료 - 회원 ID: {}", memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberMypageResponse getMypageInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        log.info("마이페이지 정보 조회 - 회원 ID: {}", memberId);
        return MemberMypageResponse.from(member);
    }

    @Override
    public MemberMypageResponse updateMonthlyLivingCost(Long memberId, Integer monthlyLivingCost) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        member.setMonthlyLivingCost(monthlyLivingCost);
        memberRepository.save(member);

        log.info("월 생활비 수정 완료 - 회원 ID: {}, 월 생활비: {}", memberId, monthlyLivingCost);
        return MemberMypageResponse.from(member);
    }
}