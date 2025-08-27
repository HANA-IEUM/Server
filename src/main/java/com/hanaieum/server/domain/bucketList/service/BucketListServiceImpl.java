package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListUpdateRequest;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.entity.BucketParticipant;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.bucketList.repository.BucketParticipantRepository;
import com.hanaieum.server.domain.group.entity.Group;
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
    private final BucketParticipantRepository bucketParticipantRepository;
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

        // 공동 버킷리스트인 경우 선택된 멤버들에게도 동일한 버킷리스트 생성
        if (requestDto.getTogetherFlag() && requestDto.getSelectedMemberIds() != null && !requestDto.getSelectedMemberIds().isEmpty()) {
            createSharedBucketLists(savedBucketList, requestDto.getSelectedMemberIds(), member);
        }

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
    
    @Override
    public List<BucketListResponse> getGroupMembersBucketLists() {
        log.info("그룹원들의 공개 버킷리스트 목록 조회 요청");
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();
        
        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        // 그룹에 속해있는지 확인
        Group group = member.getGroup();
        if (group == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }
        
        // 같은 그룹원들의 공개 버킷리스트 조회 (본인 제외)
        List<BucketList> groupBucketLists = bucketListRepository.findByMemberGroupAndPublicOrderByCreatedAtDesc(group);
        
        // 본인의 버킷리스트는 제외
        List<BucketList> otherMembersBucketLists = groupBucketLists.stream()
                .filter(bucketList -> !bucketList.getMember().getId().equals(memberId))
                .toList();
        
        log.info("그룹원들의 공개 버킷리스트 조회 완료: 총 {}개", otherMembersBucketLists.size());
        
        return otherMembersBucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    public BucketListResponse getGroupMemberBucketList(Long bucketListId) {
        log.info("그룹원의 특정 버킷리스트 조회 요청: {}", bucketListId);
        
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();
        
        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        
        // 그룹에 속해있는지 확인
        Group group = member.getGroup();
        if (group == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }
        
        // 같은 그룹 내의 공개된 버킷리스트만 조회 가능
        BucketList bucketList = bucketListRepository.findByIdAndMemberGroupAndPublic(bucketListId, group)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));
        
        log.info("그룹원 버킷리스트 조회 완료: ID = {}, 소유자 = {}", bucketListId, bucketList.getMember().getName());
        
        return BucketListResponse.of(bucketList);
    }
    
    /**
     * 공동 버킷리스트 생성 - 선택된 멤버들에게 동일한 버킷리스트 생성
     */
    @Transactional
    private void createSharedBucketLists(BucketList originalBucketList, List<Long> selectedMemberIds, Member creator) {
        log.info("공동 버킷리스트 생성 시작 - 원본 ID: {}, 대상 멤버 수: {}", 
                originalBucketList.getId(), selectedMemberIds.size());
        
        // 현재 사용자의 그룹 확인
        Group creatorGroup = creator.getGroup();
        if (creatorGroup == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }
        
        for (Long memberId : selectedMemberIds) {
            // 본인은 제외
            if (memberId.equals(creator.getId())) {
                continue;
            }
            
            // 멤버 조회 및 같은 그룹인지 확인
            Member targetMember = memberRepository.findById(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
            
            if (targetMember.getGroup() == null || !targetMember.getGroup().getId().equals(creatorGroup.getId())) {
                log.warn("다른 그룹의 멤버이므로 제외: memberId = {}", memberId);
                continue;
            }
            
            // 해당 멤버에게 동일한 버킷리스트 생성
            BucketList sharedBucketList = BucketList.builder()
                    .member(targetMember)
                    .type(originalBucketList.getType())
                    .title(originalBucketList.getTitle())
                    .targetAmount(originalBucketList.getTargetAmount())
                    .targetDate(originalBucketList.getTargetDate())
                    .publicFlag(originalBucketList.isPublicFlag())
                    .shareFlag(true) // 공동 버킷리스트이므로 항상 true
                    .status(BucketListStatus.IN_PROGRESS)
                    .deleted(false)
                    .build();
            
            BucketList savedSharedBucketList = bucketListRepository.save(sharedBucketList);
            
            // 원본 버킷리스트에 참여자로 등록 (양방향 연결)
            BucketParticipant originalParticipant = BucketParticipant.builder()
                    .bucketList(originalBucketList)
                    .member(targetMember)
                    .isActive(true)
                    .build();
            bucketParticipantRepository.save(originalParticipant);
            
            // 새로 생성된 버킷리스트에 원래 생성자를 참여자로 등록
            BucketParticipant sharedParticipant = BucketParticipant.builder()
                    .bucketList(savedSharedBucketList)
                    .member(creator)
                    .isActive(true)
                    .build();
            bucketParticipantRepository.save(sharedParticipant);
            
            log.info("공동 버킷리스트 생성 완료 - 멤버: {}, 버킷리스트 ID: {}", 
                    targetMember.getName(), savedSharedBucketList.getId());
        }
        
        log.info("공동 버킷리스트 생성 완료");
    }
    
}
