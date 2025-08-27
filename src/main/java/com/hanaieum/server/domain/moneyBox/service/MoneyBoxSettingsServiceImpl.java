package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxSettingsRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxSettingsResponse;
import com.hanaieum.server.domain.moneyBox.entity.MoneyBoxSettings;
import com.hanaieum.server.domain.moneyBox.repository.MoneyBoxSettingsRepository;
import com.hanaieum.server.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoneyBoxSettingsServiceImpl implements MoneyBoxSettingsService {
    
    private final MoneyBoxSettingsRepository moneyBoxSettingsRepository;
    private final AccountRepository accountRepository;
    private final BucketListRepository bucketListRepository;
    private final MemberRepository memberRepository;
    private final AccountService accountService;
    
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();
        
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
    
    @Override
    @Transactional
    public MoneyBoxSettingsResponse createMoneyBox(MoneyBoxSettingsRequest request) {
        log.info("머니박스 생성 요청: bucketListId = {}, boxName = {}", 
                request.getBucketListId(), request.getBoxName());
        
        Member currentMember = getCurrentMember();
        
        // 버킷리스트 조회 및 소유자 확인
        BucketList bucketList = bucketListRepository.findByIdAndDeleted(request.getBucketListId(), false)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));
        
        if (!bucketList.getMember().getId().equals(currentMember.getId())) {
            throw new CustomException(ErrorCode.BUCKET_LIST_ACCESS_DENIED);
        }
        
        // 이미 해당 버킷리스트에 대한 머니박스가 있는지 확인
        if (moneyBoxSettingsRepository.findByBucketListAndDeletedFalse(bucketList).isPresent()) {
            throw new CustomException(ErrorCode.MONEY_BOX_SETTINGS_ALREADY_EXISTS);
        }
        
        // 머니박스 계좌 생성 (MONEY_BOX 타입)
        Long accountId = accountService.createMoneyBoxAccount(
                currentMember, 
                request.getBoxName(), 
                request.getBoxName()
        );
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 머니박스 설정 생성 (중간 테이블)
        MoneyBoxSettings settings = MoneyBoxSettings.builder()
                .account(account)
                .bucketList(bucketList)
                .boxName(request.getBoxName())
                .deleted(false)
                .build();
        
        MoneyBoxSettings savedSettings = moneyBoxSettingsRepository.save(settings);
        log.info("머니박스 생성 완료: ID = {}, AccountID = {}", savedSettings.getId(), accountId);
        
        return MoneyBoxSettingsResponse.of(savedSettings);
    }
    
    @Override
    @Transactional
    public MoneyBoxSettingsResponse updateMoneyBoxSettings(Long settingsId, MoneyBoxSettingsRequest request) {
        log.info("머니박스 설정 수정 요청: settingsId = {}, boxName = {}", settingsId, request.getBoxName());
        
        Member currentMember = getCurrentMember();
        
        // 머니박스 설정 조회
        MoneyBoxSettings settings = moneyBoxSettingsRepository.findByIdAndDeletedFalse(settingsId)
                .orElseThrow(() -> new CustomException(ErrorCode.MONEY_BOX_SETTINGS_NOT_FOUND));
        
        // 소유자 확인
        if (!settings.getAccount().getMember().getId().equals(currentMember.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        // 머니박스 별명만 수정 (버킷리스트는 변경 불가)
        settings.setBoxName(request.getBoxName());
        
        MoneyBoxSettings updatedSettings = moneyBoxSettingsRepository.save(settings);
        log.info("머니박스 설정 수정 완료: ID = {}", settingsId);
        
        return MoneyBoxSettingsResponse.of(updatedSettings);
    }
    
    @Override
    @Transactional
    public void deleteMoneyBoxSettings(Long settingsId) {
        log.info("머니박스 삭제 요청: settingsId = {}", settingsId);
        
        Member currentMember = getCurrentMember();
        
        // 머니박스 설정 조회
        MoneyBoxSettings settings = moneyBoxSettingsRepository.findByIdAndDeletedFalse(settingsId)
                .orElseThrow(() -> new CustomException(ErrorCode.MONEY_BOX_SETTINGS_NOT_FOUND));
        
        // 소유자 확인
        if (!settings.getAccount().getMember().getId().equals(currentMember.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        // 계좌와 설정 모두 소프트 삭제
        Account account = settings.getAccount();
        account.setDeleted(true);
        accountRepository.save(account);
        
        settings.setDeleted(true);
        moneyBoxSettingsRepository.save(settings);
        
        log.info("머니박스 삭제 완료: SettingsID = {}, AccountID = {}", settingsId, account.getId());
    }
    
}
