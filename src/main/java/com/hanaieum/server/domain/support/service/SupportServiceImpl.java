package com.hanaieum.server.domain.support.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.support.dto.SupportRequest;
import com.hanaieum.server.domain.support.dto.SupportResponse;
import com.hanaieum.server.domain.support.entity.SupportRecord;
import com.hanaieum.server.domain.support.entity.SupportType;
import com.hanaieum.server.domain.support.repository.SupportRecordRepository;
import com.hanaieum.server.domain.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
        // 버킷리스트 존재 및 접근 권한 확인
        BucketList bucketList = bucketListRepository.findByIdAndDeletedFalse(bucketListId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        // 본인의 버킷리스트이거나 공개된 버킷리스트만 조회 가능
        if (!bucketList.getMember().getId().equals(member.getId()) && !bucketList.isPublicFlag()) {
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

}