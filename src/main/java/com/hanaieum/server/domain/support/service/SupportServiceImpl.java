package com.hanaieum.server.domain.support.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.support.dto.SupportMessageUpdateRequest;
import com.hanaieum.server.domain.support.dto.SupportRequest;
import com.hanaieum.server.domain.support.dto.SupportResponse;
import com.hanaieum.server.domain.support.entity.SupportRecord;
import com.hanaieum.server.domain.support.entity.SupportType;
import com.hanaieum.server.domain.support.repository.SupportRecordRepository;
import com.hanaieum.server.domain.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportServiceImpl implements SupportService {

    private final SupportRecordRepository supportRecordRepository;
    private final BucketListRepository bucketListRepository;
    private final TransferService transferService;

    @Override
    @Transactional
    public SupportResponse supportBucketList(Long bucketListId, SupportRequest request, Member supporter) {
        // 1. 버킷리스트 조회 및 검증
        BucketList bucketList = bucketListRepository.findByIdAndDeletedFalse(bucketListId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        // 2. 자기 자신의 버킷리스트는 후원/응원할 수 없음
        if (bucketList.getMember().getId().equals(supporter.getId())) {
            throw new CustomException(ErrorCode.CANNOT_SUPPORT_OWN_BUCKET);
        }

        // 3. 후원인 경우 추가 검증 및 이체 처리
        if (request.getSupportType() == SupportType.SPONSOR) {
            validateSponsorshipRequest(request);
            processSponsorshipTransfer(supporter, bucketList, request);
        }

        // 4. 후원/응원 기록 생성
        SupportRecord supportRecord = createSupportRecord(bucketList, supporter, request);
        supportRecord = supportRecordRepository.save(supportRecord);

        // 5. 응답 DTO 변환
        return SupportResponse.of(supportRecord);
    }

    @Override
    public List<SupportResponse> getBucketListSupports(Long bucketListId, Member member) {
        // 버킷리스트 존재 확인
        BucketList bucketList = bucketListRepository.findByIdAndDeletedFalse(bucketListId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        // 접근 권한 확인
        if (!canAccessBucketList(bucketList, member)) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }

        List<SupportRecord> supports = supportRecordRepository.findByBucketListIdWithDetails(bucketListId);
        return supports.stream()
                .map(SupportResponse::of)
                .toList();
    }

    private void validateSponsorshipRequest(SupportRequest request) {
        if (request.getSupportAmount() == null || request.getSupportAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_SUPPORT_AMOUNT);
        }

        if (request.getAccountPassword() == null || request.getAccountPassword().trim().isEmpty()) {
            throw new CustomException(ErrorCode.ACCOUNT_PASSWORD_REQUIRED);
        }
    }

    private void processSponsorshipTransfer(Member supporter, BucketList bucketList, SupportRequest request) {
        // 버킷리스트의 머니박스 계좌 조회
        Account moneyBoxAccount = bucketList.getMoneyBoxAccount();
        if (moneyBoxAccount == null) {
            throw new CustomException(ErrorCode.MONEY_BOX_NOT_FOUND);
        }

        // 이체 실행 (후원자 주계좌 → 버킷리스트 머니박스)
        // TransferService.sponsorBucket(sponsorMemberId, bucketId, amount, password)
        transferService.sponsorBucket(
                supporter.getId(),
                bucketList.getId(),
                request.getSupportAmount(),
                request.getAccountPassword()
        );
    }

    private SupportRecord createSupportRecord(BucketList bucketList, Member supporter, SupportRequest request) {
        BigDecimal supportAmount = (request.getSupportType() == SupportType.SPONSOR) 
                ? request.getSupportAmount() 
                : BigDecimal.ZERO;

        return SupportRecord.builder()
                .bucketList(bucketList)
                .supporter(supporter)
                .supportType(request.getSupportType())
                .supportAmount(supportAmount)
                .message(request.getMessage())
                .letterColor(request.getLetterColor())
                .build();
    }

    @Override
    @Transactional
    public SupportResponse updateSupportMessage(Long supportId, SupportMessageUpdateRequest request, Member member) {
        // 후원/응원 기록 조회 (삭제되지 않은 것만)
        SupportRecord supportRecord = supportRecordRepository.findByIdAndDeletedFalse(supportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUPPORT_RECORD_NOT_FOUND));

        // 작성자 본인만 수정 가능
        if (!supportRecord.getSupporter().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.SUPPORT_RECORD_ACCESS_DENIED);
        }

        // 메시지 업데이트
        supportRecord.setMessage(request.getMessage());

        supportRecordRepository.save(supportRecord);
        
        log.info("후원 메시지 수정 완료 - 후원 ID: {}, 회원 ID: {}", supportId, member.getId());
        return SupportResponse.of(supportRecord);
    }

    @Override
    public SupportResponse getSupportRecord(Long supportId, Member member) {
        // 후원/응원 기록 조회 (삭제되지 않은 것만)
        SupportRecord supportRecord = supportRecordRepository.findByIdAndDeletedFalse(supportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUPPORT_RECORD_NOT_FOUND));

        // 버킷리스트 접근 권한 확인 (작성자 본인이거나 버킷리스트에 접근할 수 있는 경우)
        if (!supportRecord.getSupporter().getId().equals(member.getId()) && 
            !canAccessBucketList(supportRecord.getBucketList(), member)) {
            throw new CustomException(ErrorCode.SUPPORT_RECORD_ACCESS_DENIED);
        }

        log.info("후원/응원 기록 조회 - 후원 ID: {}, 회원 ID: {}", supportId, member.getId());
        return SupportResponse.of(supportRecord);
    }

    @Override
    @Transactional
    public void deleteSupportRecord(Long supportId, Member member) {
        // 후원/응원 기록 조회 (삭제되지 않은 것만)
        SupportRecord supportRecord = supportRecordRepository.findByIdAndDeletedFalse(supportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUPPORT_RECORD_NOT_FOUND));

        // 작성자 본인만 삭제 가능
        if (!supportRecord.getSupporter().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.SUPPORT_RECORD_ACCESS_DENIED);
        }

        // Soft delete 처리
        supportRecord.setDeleted(true);
        supportRecordRepository.save(supportRecord);

        log.info("후원/응원 기록 삭제 완료 - 후원 ID: {}, 회원 ID: {}", supportId, member.getId());
    }

    /**
     * 버킷리스트에 접근할 수 있는지 확인
     * 1. 본인의 버킷리스트인 경우 무조건 가능
     * 2. 공개된 버킷리스트이면 같은 그룹에 속한 경우 가능
     * 3. 공개되지 않은 버킷리스트이면 BucketParticipant에 속해야 함
     */
    private boolean canAccessBucketList(BucketList bucketList, Member member) {
        // 1. 본인의 버킷리스트인 경우 무조건 허용
        if (bucketList.getMember().getId().equals(member.getId())) {
            return true;
        }

        // 2. 공개된 버킷리스트인 경우
        if (bucketList.isPublicFlag()) {
            // 같은 그룹에 속한지 확인
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

}