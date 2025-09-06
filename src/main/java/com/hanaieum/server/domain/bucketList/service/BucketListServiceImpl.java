package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.autoTransfer.service.AutoTransferScheduleService;
import com.hanaieum.server.domain.bucketList.calculator.InterestCalculator;
import com.hanaieum.server.domain.coupon.service.CouponService;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import com.hanaieum.server.domain.transaction.service.TransactionService;
import com.hanaieum.server.domain.transfer.service.TransferService;
import com.hanaieum.server.domain.bucketList.dto.*;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import static java.lang.Integer.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BucketListServiceImpl implements BucketListService {

    private final BucketListRepository bucketListRepository;
    private final BucketParticipantRepository bucketParticipantRepository;
    private final MemberRepository memberRepository;

    private final AccountService accountService;
    private final TransferService transferService;
    private final TransactionService transactionService;
    private final AutoTransferScheduleService autoTransferScheduleService;
    private final CouponService couponService;

    private final InterestCalculator interestCalculator;

    /**
     * 현재 로그인한 사용자 정보를 가져오는 공통 메서드
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();

        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 버킷리스트 소유자 권한 확인 공통 메서드
     */
    private void validateBucketListOwnership(BucketList bucketList, Long memberId) {
        if (!bucketList.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }
    }

    /**
     * 삭제되지 않은 버킷리스트 조회 (소유자 확인 포함)
     */
    private BucketList getBucketListWithOwnershipValidation(Long bucketListId, Long memberId) {
        BucketList bucketList = bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        validateBucketListOwnership(bucketList, memberId);
        return bucketList;
    }

    /**
     * 현재 로그인한 사용자의 버킷리스트 조회
     */
    @NotNull
    private BucketList getBucketList(Member member, Long bucketListId) {
        Long memberId = member.getId();

        // 삭제되지 않은 버킷리스트만 조회 (참여자 정보 포함)
        BucketList bucketList = bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        // 소유자 확인
        if (!bucketList.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }
        return bucketList;
    }

    @Override
    @Transactional
    public BucketListResponse createBucketList(BucketListRequest requestDto) {
        log.info("버킷리스트 생성 요청: {}", requestDto.getTitle());

        Member member = getCurrentMember();

        // targetMonths를 현재 날짜에 더해서 targetDate 계산
        int months = parseInt(requestDto.getTargetMonths());
        LocalDate targetDate = LocalDate.now().plusMonths(months);

        // 버킷리스트 엔티티 생성
        BucketList bucketList = BucketList.builder()
                .member(member)
                .type(requestDto.getType())
                .title(requestDto.getTitle())
                .targetAmount(requestDto.getTargetAmount())
                .targetMonth(months)
                .targetDate(targetDate)
                .publicFlag(requestDto.getPublicFlag())
                .shareFlag(requestDto.getTogetherFlag())
                .status(BucketListStatus.IN_PROGRESS) // 생성 시 기본값: 진행중
                .deleted(false) // 기본값: 활성화
                .build();

        // 저장
        BucketList savedBucketList = bucketListRepository.save(bucketList);
        log.info("버킷리스트 생성 완료: ID = {}", savedBucketList.getId());

        // 공유 버킷리스트인 경우 선택된 멤버들을 참여자로 추가
        if (requestDto.getTogetherFlag() && requestDto.getSelectedMemberIds() != null && !requestDto.getSelectedMemberIds().isEmpty()) {
            try {
                updateBucketListParticipants(savedBucketList, requestDto.getSelectedMemberIds());
                log.info("공유 버킷리스트 참여자 추가 완료: bucketListId = {}, 참여자 수 = {}",
                        savedBucketList.getId(), requestDto.getSelectedMemberIds().size());
            } catch (Exception e) {
                log.warn("참여자 추가 실패 (버킷리스트 생성은 완료됨): bucketListId = {}, error = {}",
                        savedBucketList.getId(), e.getMessage());
                // 참여자 추가 실패해도 버킷리스트 생성은 성공 처리
            }
            }

        // 머니박스 자동 생성
        if (requestDto.getCreateMoneyBox() != null && requestDto.getCreateMoneyBox()) {
            try {
                // 자동이체 정보가 있는 경우 자동이체 포함하여 생성
                if (Boolean.TRUE.equals(requestDto.getEnableAutoTransfer()) &&
                        requestDto.getMonthlyAmount() != null &&
                        requestDto.getTransferDay() != null) {

                    Integer transferDay = parseInt(requestDto.getTransferDay());
                    accountService.createMoneyBoxForBucketList(
                            savedBucketList,
                            member,
                            requestDto.getMoneyBoxName(),
                            requestDto.getEnableAutoTransfer(),
                            requestDto.getMonthlyAmount(),
                            transferDay
                    );
                    log.info("버킷리스트와 연동된 머니박스 및 자동이체 생성 완료: bucketListId = {}, monthlyAmount = {}, transferDay = {}일",
                            savedBucketList.getId(), requestDto.getMonthlyAmount(), transferDay);
                } else {
                    // 자동이체 없이 머니박스만 생성
                    accountService.createMoneyBoxForBucketList(
                            savedBucketList,
                            member,
                            requestDto.getMoneyBoxName()
                    );
                    log.info("버킷리스트와 연동된 머니박스 생성 완료: bucketListId = {}", savedBucketList.getId());
                }
            } catch (Exception e) {
                log.warn("머니박스 자동 생성 실패 (버킷리스트 생성은 완료됨): bucketListId = {}, error = {}",
                        savedBucketList.getId(), e.getMessage());
                // 머니박스 생성 실패해도 버킷리스트 생성은 성공으로 처리
            }
        }

        return BucketListResponse.of(savedBucketList);
    }

    @Override
    public List<BucketListResponse> getInProgressBucketLists() {
        log.info("진행중인 버킷리스트 목록 조회 요청");

        Member member = getCurrentMember();

        // 삭제되지 않은 해당 회원의 진행중인 버킷리스트 조회 (생성일 기준 내림차순)
        List<BucketList> bucketLists = bucketListRepository.findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(
                member, BucketListStatus.IN_PROGRESS, false);

        log.info("진행중인 버킷리스트 목록 조회 완료: 총 {}개", bucketLists.size());

        return bucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    public List<BucketListResponse> getCompletedBucketLists() {
        log.info("완료된 버킷리스트 목록 조회 요청");

        Member member = getCurrentMember();

        // 삭제되지 않은 해당 회원의 완료된 버킷리스트 조회 (생성일 기준 내림차순)
        List<BucketList> bucketLists = bucketListRepository.findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(
                member, BucketListStatus.COMPLETED, false);

        log.info("완료된 버킷리스트 목록 조회 완료: 총 {}개", bucketLists.size());

        return bucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    public List<BucketListResponse> getParticipatedBucketLists() {
        log.info("참여중인 버킷리스트 목록 조회 요청");

        Member member = getCurrentMember();
        Long memberId = member.getId();

        // 삭제되지 않은 해당 회원이 참여중인 버킷리스트 조회 (참여일 기준 내림차순)
        List<BucketList> bucketLists = bucketListRepository.findByParticipantMemberId(memberId);

        log.info("참여중인 버킷리스트 목록 조회 완료: 총 {}개", bucketLists.size());

        return bucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    public MyBucketListDetailResponse getBucketListDetail(Long bucketListId) {
        log.info("내 버킷리스트 상세조회 요청: {}", bucketListId);

        BucketList bucketList = getBucketList(getCurrentMember(), bucketListId);

        log.info("버킷리스트 상세조회 완료: ID = {}, 제목 = {}", bucketListId, bucketList.getTitle());

        return MyBucketListDetailResponse.of(bucketList);
    }

    @Override
    public List<BucketListResponse> getGroupInProgressBucketLists(Long groupMemberId) {
        log.info("그룹원의 진행중인 버킷리스트 목록 조회 요청: groupMemberId = {}", groupMemberId);

        Member currentMember = getCurrentMember();

        // 그룹에 속해있는지 확인
        Group group = currentMember.getGroup();
        if (group == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 대상 그룹원 조회
        Member targetMember = memberRepository.findById(groupMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 같은 그룹에 속해있는지 확인
        if (targetMember.getGroup() == null || !targetMember.getGroup().getId().equals(group.getId())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 1. 그룹원의 모든 진행중인 버킷리스트 조회 (공개 여부 상관없이)
        List<BucketList> allBucketLists = bucketListRepository.findByGroupMemberIdAndInProgress(groupMemberId);

        // 2. 본인이 참여자인 비공개 진행중인 버킷리스트 조회
        List<BucketList> participatedBucketLists = bucketListRepository.findByParticipantMemberId(currentMember.getId())
                .stream()
                .filter(bl -> bl.getMember().getId().equals(groupMemberId) && bl.getStatus() == BucketListStatus.IN_PROGRESS)
                .toList();

        // 3. 접근 가능한 버킷리스트 필터링 (공개된 것 + 참여자인 것)
        List<BucketList> accessibleBucketLists = new ArrayList<>();

        for (BucketList bucketList : allBucketLists) {
            // 공개된 버킷리스트이거나 본인이 참여자인 경우
            if (bucketList.isPublicFlag() ||
                participatedBucketLists.stream().anyMatch(p -> p.getId().equals(bucketList.getId()))) {
                accessibleBucketLists.add(bucketList);
            }
        }

        // 4. 생성일 기준으로 정렬
        accessibleBucketLists.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        log.info("그룹원의 진행중인 버킷리스트 목록 조회 완료: groupMemberId = {}, 총 {}개 (전체: {}, 접근가능: {})",
                groupMemberId, accessibleBucketLists.size(), allBucketLists.size(), accessibleBucketLists.size());

        return accessibleBucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    public List<BucketListResponse> getGroupCompletedBucketLists(Long groupMemberId) {
        log.info("그룹원의 완료된 버킷리스트 목록 조회 요청: groupMemberId = {}", groupMemberId);

        Member currentMember = getCurrentMember();

        // 그룹에 속해있는지 확인
        Group group = currentMember.getGroup();
        if (group == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 대상 그룹원 조회
        Member targetMember = memberRepository.findById(groupMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 같은 그룹에 속해있는지 확인
        if (targetMember.getGroup() == null || !targetMember.getGroup().getId().equals(group.getId())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 1. 그룹원의 모든 완료된 버킷리스트 조회 (공개 여부 상관없이)
        List<BucketList> allBucketLists = bucketListRepository.findByGroupMemberIdAndCompleted(groupMemberId);

        // 2. 본인이 참여자인 비공개 완료된 버킷리스트 조회
        List<BucketList> participatedBucketLists = bucketListRepository.findByParticipantMemberId(currentMember.getId())
                .stream()
                .filter(bl -> bl.getMember().getId().equals(groupMemberId) && bl.getStatus() == BucketListStatus.COMPLETED)
                .toList();

        // 3. 접근 가능한 버킷리스트 필터링 (공개된 것 + 참여자인 것)
        List<BucketList> accessibleBucketLists = new ArrayList<>();

        for (BucketList bucketList : allBucketLists) {
            // 공개된 버킷리스트이거나 본인이 참여자인 경우
            if (bucketList.isPublicFlag() ||
                participatedBucketLists.stream().anyMatch(p -> p.getId().equals(bucketList.getId()))) {
                accessibleBucketLists.add(bucketList);
            }
        }

        // 4. 생성일 기준으로 정렬
        accessibleBucketLists.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        log.info("그룹원의 완료된 버킷리스트 목록 조회 완료: groupMemberId = {}, 총 {}개 (전체: {}, 접근가능: {})",
                groupMemberId, accessibleBucketLists.size(), allBucketLists.size(), accessibleBucketLists.size());

        return accessibleBucketLists.stream()
                .map(BucketListResponse::of)
                .toList();
    }

    @Override
    public GroupBucketListDetailResponse getGroupMemberBucketList(Long bucketListId) {
        log.info("그룹원의 특정 버킷리스트 조회 요청: {}", bucketListId);

        Member member = getCurrentMember();

        // 삭제되지 않은 버킷리스트 조회 (참여자 정보 포함)
        BucketList bucketList = bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        // 1. 본인의 버킷리스트인 경우 접근 거부 (이 API는 남의 버킷리스트 조회용)
        if (bucketList.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }

        // 접근 권한 검증 (공개 버킷리스트 또는 참여자 권한)
        if (!canAccessOthersBucketList(bucketList, member)) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }

        log.info("그룹원 버킷리스트 조회 완료: ID = {}, 소유자 = {}", bucketListId, bucketList.getMember().getName());

        return GroupBucketListDetailResponse.of(bucketList);
    }

    /**
     * 다른 사람의 버킷리스트 접근 권한 검증 (본인 제외)
     */
    private boolean canAccessOthersBucketList(BucketList bucketList, Member member) {
        // 2. 공개된 버킷리스트인 경우 - 같은 그룹 확인
        if (bucketList.isPublicFlag()) {
            if (bucketList.getMember().getGroup() != null && member.getGroup() != null) {
                return bucketList.getMember().getGroup().getId().equals(member.getGroup().getId());
            }
            return false;
        }

        // 3. 비공개 버킷리스트인 경우 - 참여자인지 확인
        return bucketList.getParticipants().stream()
                .anyMatch(participant ->
                        participant.getMember().getId().equals(member.getId()) &&
                                participant.getActive()
                );
    }

    @Override
    @Transactional
    public BucketListResponse updateBucketList(Long bucketListId, BucketListUpdateRequest requestDto) {
        log.info("버킷리스트 수정 요청: {} - {}", bucketListId, requestDto.getTitle());

        BucketList bucketList = getBucketList(getCurrentMember(), bucketListId);

        // 제목 수정
        bucketList.setTitle(requestDto.getTitle());

        // 공개여부 수정 (null이 아닌 경우에만)
        if (requestDto.getPublicFlag() != null) {
            bucketList.setPublicFlag(requestDto.getPublicFlag());
            log.info("공개여부 수정: {}", requestDto.getPublicFlag());
        }

        // 혼자/같이 진행 여부 수정
        if (requestDto.getShareFlag() != null) {
            boolean previousShareFlag = bucketList.isShareFlag();
            bucketList.setShareFlag(requestDto.getShareFlag());
            log.info("혼자/같이 진행 여부 수정: {} -> {}", previousShareFlag, requestDto.getShareFlag());

            // 같이 진행으로 변경된 경우, 선택된 멤버들과 공유
            if (requestDto.getShareFlag() && !previousShareFlag) {
                if (requestDto.getSelectedMemberIds() != null && !requestDto.getSelectedMemberIds().isEmpty()) {
                    updateBucketListParticipants(bucketList, requestDto.getSelectedMemberIds());
                }
            }
            // 혼자 진행으로 변경된 경우, 기존 참여자들 비활성화
            else if (!requestDto.getShareFlag() && previousShareFlag) {
                deactivateAllParticipants(bucketList);
            }
            // 이미 같이 진행 중이고 멤버 목록이 변경된 경우
            else if (requestDto.getShareFlag() && previousShareFlag) {
                if (requestDto.getSelectedMemberIds() != null) {
                    updateBucketListParticipants(bucketList, requestDto.getSelectedMemberIds());
                }
            }
        }

        BucketList savedBucketList = bucketListRepository.save(bucketList);
        log.info("버킷리스트 수정 완료: ID = {}", bucketListId);

        return BucketListResponse.of(savedBucketList);
    }


    /**
     * 버킷리스트 참여자 업데이트
     */
    @Transactional
    protected void updateBucketListParticipants(BucketList bucketList, List<Long> selectedMemberIds) {
        log.info("버킷리스트 참여자 업데이트 - 버킷리스트 ID: {}, 선택된 멤버 수: {}",
                bucketList.getId(), selectedMemberIds.size());

        Member owner = bucketList.getMember();
        Group ownerGroup = owner.getGroup();

        if (ownerGroup == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 기존 참여자들을 모두 비활성화
        List<BucketParticipant> existingParticipants = bucketParticipantRepository.findByBucketListAndActive(bucketList, true);
        for (BucketParticipant participant : existingParticipants) {
            participant.setActive(false);
        }
        bucketParticipantRepository.saveAll(existingParticipants);

        // 새로운 참여자들 추가
        for (Long memberId : selectedMemberIds) {
            // 본인은 제외
            if (memberId.equals(owner.getId())) {
                continue;
            }

            // 멤버 조회 및 같은 그룹인지 확인
            Member targetMember = memberRepository.findById(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            if (targetMember.getGroup() == null || !targetMember.getGroup().getId().equals(ownerGroup.getId())) {
                log.warn("다른 그룹의 멤버이므로 제외: memberId = {}", memberId);
                continue;
            }

            // 기존에 이미 참여자였는지 확인
            BucketParticipant existingParticipant = bucketParticipantRepository
                    .findByBucketListAndMember(bucketList, targetMember)
                    .orElse(null);

            if (existingParticipant != null) {
                // 기존 참여자를 다시 활성화
                existingParticipant.setActive(true);
                bucketParticipantRepository.save(existingParticipant);
            } else {
                // 새로운 참여자 추가
                BucketParticipant newParticipant = BucketParticipant.builder()
                        .bucketList(bucketList)
                        .member(targetMember)
                        .active(true)
                        .build();
                bucketParticipantRepository.save(newParticipant);
            }

            log.info("참여자 추가/활성화: 멤버 = {}", targetMember.getName());
        }

        log.info("버킷리스트 참여자 업데이트 완료");
    }

    /**
     * 모든 참여자 비활성화 (혼자 진행으로 변경 시)
     */
    @Transactional
    protected void deactivateAllParticipants(BucketList bucketList) {
        log.info("모든 참여자 비활성화 - 버킷리스트 ID: {}", bucketList.getId());

        List<BucketParticipant> participants = bucketParticipantRepository.findByBucketListAndActive(bucketList, true);
        for (BucketParticipant participant : participants) {
            participant.setActive(false);
        }
        bucketParticipantRepository.saveAll(participants);

        log.info("모든 참여자 비활성화 완료: {}명", participants.size());
    }

    @Override
    @Transactional
    public void deleteBucketList(Long bucketListId) {
        log.info("버킷리스트 삭제 요청: {}", bucketListId);

        Member member = getCurrentMember();
        BucketList bucketList = getBucketListWithOwnershipValidation(bucketListId, member.getId());

        // 진행중인 버킷리스트
        if (bucketList.getStatus() == BucketListStatus.IN_PROGRESS) {
            Account moneyBoxAccount = bucketList.getMoneyBoxAccount();
            log.info("연결된 머니박스: {}", moneyBoxAccount.getId());
            
            // 진행중인 버킷리스트인 경우 머니박스의 잔액을 주계좌로 반환
            if (bucketList.getStatus() == BucketListStatus.IN_PROGRESS) {
                // 머니박스 → 주계좌 전액 인출
                BigDecimal withdrawnAmount = transferService.withdrawAllFromMoneyBox(
                        member.getId(),
                        moneyBoxAccount.getId(),
                        bucketListId
                );
                
                log.info("버킷리스트 삭제로 인한 머니박스 잔액 인출 완료: {} → 주계좌, 인출금액: {}", 
                        moneyBoxAccount.getId(), withdrawnAmount);
            }
            
            // 머니박스 계좌 삭제
            moneyBoxAccount.setDeleted(true);
            accountService.save(moneyBoxAccount);
            log.info("머니박스 계좌 삭제 완료: {}", moneyBoxAccount.getId());
            
            // 관련 자동이체 스케줄 모두 삭제 및 비활성화
            autoTransferScheduleService.deleteAllSchedulesForMoneyBox(moneyBoxAccount);
            log.info("자동이체 스케줄 삭제 완료");
        }

        // 버킷리스트 삭제
        bucketList.setDeleted(true);
        bucketListRepository.save(bucketList);

        log.info("버킷리스트 삭제 완료: ID = {}", bucketListId);
    }

    /**
     * 목표 달성 이자율 계산 (기간 기준 + 연금통장 우대 이자율)
     */
    private BigDecimal calculateInterestRate(Integer targetMonth, Account mainAccount) {
        // 기본 이자율 계산
        BigDecimal baseRate = switch (targetMonth) {
            case 3  -> BigDecimal.valueOf(0.010); // 1.0%
            case 6  -> BigDecimal.valueOf(0.015); // 1.5%
            case 12 -> BigDecimal.valueOf(0.025); // 2.5%
            case 24 -> BigDecimal.valueOf(0.032); // 3.2%
            default -> BigDecimal.valueOf(0.010); // 기본값: 3개월
        };
        
        // 연금통장 우대 이자율 적용 (+1.0%)
        if (mainAccount.getName().contains("연금")) {
            BigDecimal originalRate = baseRate;
            baseRate = baseRate.add(BigDecimal.valueOf(0.010));
            log.info("연금통장 우대이자 적용 - 기간: {}개월, 기본: {}% + 우대: 1.0% = 최종: {}%", 
                    targetMonth, originalRate.multiply(BigDecimal.valueOf(100)), 
                    baseRate.multiply(BigDecimal.valueOf(100)));
        } else {
            log.info("이자율 계산 완료 - 기간: {}개월, 이자율: {}%", 
                    targetMonth, baseRate.multiply(BigDecimal.valueOf(100)));
        }
        
        return baseRate;
    }

    @Override
    @Transactional
    public BucketListResponse completeBucketList(Long bucketListId) {
        log.info("버킷리스트 완료 처리 요청: {}", bucketListId);

        Member member = getCurrentMember();
        BucketList bucketList = getBucketListWithOwnershipValidation(bucketListId, member.getId());

        // 이미 완료된 버킷리스트인지 확인
        if (bucketList.getStatus() == BucketListStatus.COMPLETED) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ALREADY_COMPLETED);
        }

        // 오늘 날짜가 targetDate 이전이면 아직 달성 불가
        if (LocalDate.now().isBefore(bucketList.getTargetDate())) {
            throw new CustomException(ErrorCode.BUCKET_LIST_NOT_YET_AVAILABLE);
        }

        // 주계좌 조회
        Account mainAccount = accountService.findMainAccountByMember(member);
        
        // 연결된 머니박스가 있는지 확인
        Account moneyBoxAccount = bucketList.getMoneyBoxAccount();
        
        if (moneyBoxAccount != null) {
            // 1. 머니박스 → 주계좌로 원금 인출
            BigDecimal withdrawnAmount = transferService.withdrawAllFromMoneyBox(
                    member.getId(),
                    moneyBoxAccount.getId(),
                    bucketListId
            );
            log.info("목표 달성 원금 인출 완료: 머니박스 {} → 주계좌, 인출금액: {}", 
                    moneyBoxAccount.getId(), withdrawnAmount);
            
            // 2. 이자 계산 및 지급 (목표금액 한도 내에서만)
            BigDecimal interestRate = calculateInterestRate(bucketList.getTargetMonth(), mainAccount);

            // 버킷리스트의 머니박스 계좌에 입금된 내역 조회(targetDate 이전에 발생한 내역들만 createdAt 순서대로)
            List<Transaction> transactions = transactionService.getTransactionsByTransactionType(moneyBoxAccount, TransactionType.DEPOSIT, bucketList.getTargetDate());

            // 단리 이자 계산
            BigDecimal interest = interestCalculator.calculateInterest(transactions, bucketList.getTargetDate(), bucketList.getTargetAmount(), interestRate);

            log.info("이자 계산: 인출금액 = {}, 목표금액 = {}, 이자율 = {}%, 계산된 이자 = {}",
                    withdrawnAmount, bucketList.getTargetAmount(), interestRate, interest);

            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                transferService.payInterest(member.getId(), interest, bucketListId);
            }
            
            // 3. 머니박스 계좌 삭제
            moneyBoxAccount.setDeleted(true);
            accountService.save(moneyBoxAccount);
            log.info("머니박스 계좌 삭제 완료: {}", moneyBoxAccount.getId());
            
            // 4. 관련 자동이체 스케줄 모두 삭제 및 비활성화
            autoTransferScheduleService.deleteAllSchedulesForMoneyBox(moneyBoxAccount);
            log.info("자동이체 스케줄 삭제 완료");
        }

        // 5. 버킷리스트 상태를 완료로 변경
        BucketListStatus previousStatus = bucketList.getStatus();
        bucketList.setStatus(BucketListStatus.COMPLETED);
        BucketList savedBucketList = bucketListRepository.save(bucketList);

        // 6. 쿠폰 발행
        try {
            couponService.createMemberCoupon(bucketListId);
            log.info("버킷리스트 달성 쿠폰 발행 완료: bucketListId = {}", bucketListId);
        } catch (Exception e) {
            log.warn("쿠폰 발행 실패 (버킷리스트 달성은 완료됨): bucketListId = {}, error = {}", bucketListId, e.getMessage());
            // 쿠폰 발행 실패해도 버킷리스트 달성은 성공으로 처리
        }

        log.info("버킷리스트 완료 처리 완료: ID = {}, {} -> COMPLETED", bucketListId, previousStatus);

        return BucketListResponse.of(savedBucketList);
    }

    @Override
    public BucketListCreationAvailabilityResponse checkCreationAvailability() {
        log.info("버킷리스트 생성 가능 여부 확인 요청");

        Member member = getCurrentMember();

        // 현재 사용자의 삭제되지 않은 머니박스 개수 조회
        long currentMoneyBoxCount = accountService.getMoneyBoxCountByMember(member);

        // 최대 허용 개수 (20개)
        int maxLimit = 20;
        boolean canCreate = currentMoneyBoxCount < maxLimit;

        log.info("버킷리스트 생성 가능 여부 확인 완료: 현재 {}개, 최대 {}개, 생성가능 = {}",
                currentMoneyBoxCount, maxLimit, canCreate);

        return new BucketListCreationAvailabilityResponse(canCreate, currentMoneyBoxCount);
    }

}
