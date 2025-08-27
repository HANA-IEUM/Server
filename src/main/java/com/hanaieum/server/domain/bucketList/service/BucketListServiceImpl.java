package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListUpdateRequest;
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

import java.time.LocalDate;
import java.util.List;

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

        // targetMonths를 현재 날짜에 더해서 targetDate 계산
        int months = Integer.parseInt(requestDto.getTargetMonths());
        LocalDate targetDate = LocalDate.now().plusMonths(months);

        // 버킷리스트 엔티티 생성
        BucketList bucketList = BucketList.builder()
                .member(member)
                .type(requestDto.getType())
                .title(requestDto.getTitle())
                .targetAmount(requestDto.getTargetAmount())
                .targetDate(targetDate)
                .publicFlag(requestDto.getPublicFlag())
                .shareFlag(requestDto.getTogetherFlag())
                .status(BucketListStatus.IN_PROGRESS) // 생성 시 기본값: 진행중
                .deleted(false) // 기본값: 활성화
                .build();

        // 저장
        BucketList savedBucketList = bucketListRepository.save(bucketList);
        log.info("버킷리스트 생성 완료: ID = {}", savedBucketList.getId());

        return BucketListResponse.of(savedBucketList);
    }

    @Override
    public List<BucketListResponse> getBucketLists() {
        log.info("버킷리스트 목록 조회 요청");
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();
        
        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        // 삭제되지 않은 해당 회원의 버킷리스트 조회 (생성일 기준 내림차순)
        List<BucketList> bucketLists = bucketListRepository.findByMemberAndDeletedOrderByCreatedAtDesc(member, false);
        
        log.info("버킷리스트 목록 조회 완료: 총 {}개", bucketLists.size());

        return bucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public void deleteBucketList(Long bucketListId) {
        log.info("버킷리스트 삭제 요청: {}", bucketListId);
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();
        
        // 삭제되지 않은 버킷리스트만 조회
        BucketList bucketList = bucketListRepository.findByIdAndDeleted(bucketListId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));
        
        // 소유자 확인
        if (!bucketList.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }
        
        // 소프트 삭제 (deleted 플래그를 true로 변경)
        bucketList.setDeleted(true);
        
        bucketListRepository.save(bucketList);
        log.info("버킷리스트 삭제 완료: ID = {}", bucketListId);
    }

    @Override
    @Transactional
    public BucketListResponse updateBucketList(Long bucketListId, BucketListUpdateRequest requestDto) {
        log.info("버킷리스트 수정 요청: {} - {}", bucketListId, requestDto.getTitle());
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();
        
        // 삭제되지 않은 버킷리스트만 조회
        BucketList bucketList = bucketListRepository.findByIdAndDeleted(bucketListId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));
        
        // 소유자 확인
        if (!bucketList.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }
        
        // 제목만 수정 (현재 요구사항)
        bucketList.setTitle(requestDto.getTitle());
        
        BucketList savedBucketList = bucketListRepository.save(bucketList);
        log.info("버킷리스트 수정 완료: ID = {}", bucketListId);
        
        return BucketListResponse.of(savedBucketList);
    }
    
}
