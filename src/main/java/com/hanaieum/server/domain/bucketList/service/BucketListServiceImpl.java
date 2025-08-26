package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BucketListServiceImpl implements BucketListService {

    private final BucketListRepository bucketListRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public BucketListResponse createBucketList(BucketListRequest requestDto) {
        log.info("버킷리스트 생성 요청: {}", requestDto.getTitle());

        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 버킷리스트 엔티티 생성
        BucketList bucketList = BucketList.builder()
                .member(member)
                .type(requestDto.getType())
                .title(requestDto.getTitle())
                .targetAmount(requestDto.getTargetAmount())
                .targetDate(requestDto.getTargetDate())
                .publicFlag(requestDto.getPublicFlag())
                .shareFlag(requestDto.getTogetherFlag())
                .status(BucketListStatus.IN_PROGRESS) // 생성 시 기본값: 진행중
                .active(true) // 기본값: 활성화
                .build();

        // 저장
        BucketList savedBucketList = bucketListRepository.save(bucketList);
        log.info("버킷리스트 생성 완료: ID = {}", savedBucketList.getId());

        // Response DTO로 변환하여 반환
        return BucketListResponse.from(savedBucketList);
    }


}
