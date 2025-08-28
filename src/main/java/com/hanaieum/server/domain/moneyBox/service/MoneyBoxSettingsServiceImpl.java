package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferScheduleRepository;
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
import java.util.Optional;

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
    private final AutoTransferScheduleRepository autoTransferScheduleRepository;

    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getId();

        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    @Transactional
    public MoneyBoxSettingsResponse createMoneyBoxForBucketList(BucketList bucketList, Member member, String boxName) {
        log.info("버킷리스트 연동 머니박스 생성 요청: bucketListId = {}, memberId = {}, boxName = {}",
                bucketList.getId(), member.getId(), boxName);

        // 이미 해당 버킷리스트에 대한 머니박스가 있는지 확인
        if (moneyBoxSettingsRepository.findByBucketListAndDeletedFalse(bucketList).isPresent()) {
            log.warn("이미 해당 버킷리스트에 대한 머니박스가 존재함: bucketListId = {}", bucketList.getId());
            throw new CustomException(ErrorCode.MONEY_BOX_SETTINGS_ALREADY_EXISTS);
        }

        // 머니박스 이름 결정 (없으면 버킷리스트 제목 사용)
        String finalBoxName = (boxName != null && !boxName.trim().isEmpty()) ? boxName.trim() : bucketList.getTitle();

        // 머니박스 계좌 생성 (MONEY_BOX 타입)
        Long accountId = accountService.createMoneyBoxAccount(member, finalBoxName);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 머니박스 설정 생성 (중간 테이블)
        MoneyBoxSettings settings = MoneyBoxSettings.builder()
                .account(account)
                .bucketList(bucketList)
                .boxName(finalBoxName)
                .deleted(false)
                .build();

        MoneyBoxSettings savedSettings = moneyBoxSettingsRepository.save(settings);
        log.info("버킷리스트 연동 머니박스 생성 완료: ID = {}, AccountID = {}", savedSettings.getId(), accountId);

        return MoneyBoxSettingsResponse.of(savedSettings);
    }

    @Override
    @Transactional
    public MoneyBoxSettingsResponse createMoneyBox(MoneyBoxSettingsRequest request) {
        log.warn("⚠️ Deprecated API 호출: 머니박스 개별 생성 API 사용됨. 버킷리스트 생성 시 createMoneyBox 옵션 사용을 권장합니다.");
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
        log.info("머니박스 설정 수정 요청: settingsId = {}, boxName = {}, autoTransferEnabled = {}, monthlyAmount = {}, transferDay = {}", 
                settingsId, request.getBoxName(), request.getAutoTransferEnabled(), 
                request.getMonthlyPaymentAmount(), request.getAutoTransferDay());

        Member currentMember = getCurrentMember();

        // 머니박스 설정 조회
        MoneyBoxSettings settings = moneyBoxSettingsRepository.findByIdAndDeletedFalse(settingsId)
                .orElseThrow(() -> new CustomException(ErrorCode.MONEY_BOX_SETTINGS_NOT_FOUND));

        // 소유자 확인
        if (!settings.getAccount().getMember().getId().equals(currentMember.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 머니박스 별명 수정 및 연결된 계좌 정보 업데이트
        if (request.getBoxName() != null) {
            String newBoxName = request.getBoxName();
            
            // MoneyBoxSettings의 별명 수정
            settings.setBoxName(newBoxName);

            // 연결된 Account의 이름 수정
            Account account = settings.getAccount();
            account.setName(newBoxName);      // 계좌명 변경
            accountRepository.save(account);
        }

        // 자동이체 설정 수정
        updateAutoTransferSettings(currentMember, settings.getAccount(), request);

        MoneyBoxSettings updatedSettings = moneyBoxSettingsRepository.save(settings);
        log.info("머니박스 설정 수정 완료: ID = {}, 새 이름 = {}", settingsId, request.getBoxName());

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

        // 관련된 자동이체 스케줄도 함께 삭제
        deleteRelatedAutoTransferSchedules(currentMember, settings.getAccount());

        // 계좌와 설정 모두 소프트 삭제
        Account account = settings.getAccount();
        account.setDeleted(true);
        accountRepository.save(account);

        settings.setDeleted(true);
        moneyBoxSettingsRepository.save(settings);

        log.info("머니박스 삭제 완료: SettingsID = {}, AccountID = {}", settingsId, account.getId());
    }

    @Override
    public List<MoneyBoxSettingsResponse> getMyMoneyBoxList() {
        log.info("내 머니박스 목록 조회 요청");

        Member currentMember = getCurrentMember();

        List<MoneyBoxSettings> settingsList = moneyBoxSettingsRepository
                .findByMemberAndDeletedFalse(currentMember);

        List<MoneyBoxSettingsResponse> responses = settingsList.stream()
                .map(MoneyBoxSettingsResponse::of)
                .toList();

        log.info("내 머니박스 목록 조회 완료: {} 개", responses.size());

        return responses;
    }

    /**
     * 자동이체 설정 수정
     */
    @Transactional
    protected void updateAutoTransferSettings(Member member, Account toAccount, MoneyBoxSettingsRequest request) {
        try {
            log.info("자동이체 설정 수정 시작: memberId = {}, toAccountId = {}, enabled = {}", 
                    member.getId(), toAccount.getId(), request.getAutoTransferEnabled());
            
            // 주계좌 조회 (출금 계좌)
            Account fromAccount = accountRepository.findByMemberAndAccountTypeAndDeletedFalse(
                    member, AccountType.MAIN)
                    .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
            
            // 기존 자동이체 스케줄 조회
            Optional<AutoTransferSchedule> existingSchedule = autoTransferScheduleRepository
                    .findByFromAccountAndToAccountAndActiveTrueAndDeletedFalse(fromAccount, toAccount);
            
            // 자동이체 비활성화 요청인 경우
            if (request.getAutoTransferEnabled() != null && !request.getAutoTransferEnabled()) {
                if (existingSchedule.isPresent()) {
                    AutoTransferSchedule schedule = existingSchedule.get();
                    schedule.setActive(false);
                    autoTransferScheduleRepository.save(schedule);
                    log.info("자동이체 스케줄 비활성화: scheduleId = {}", schedule.getId());
                }
                return;
            }
            
            // 자동이체 활성화 요청인 경우
            if (request.getAutoTransferEnabled() != null && request.getAutoTransferEnabled()) {
                // 필수 파라미터 검증
                if (request.getMonthlyPaymentAmount() == null || request.getAutoTransferDay() == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                }
                
                if (existingSchedule.isPresent()) {
                    // 기존 스케줄 수정
                    AutoTransferSchedule schedule = existingSchedule.get();
                    schedule.setAmount(request.getMonthlyPaymentAmount());
                    schedule.setTransferDay(request.getAutoTransferDay());
                    schedule.setActive(true);
                    autoTransferScheduleRepository.save(schedule);
                    log.info("자동이체 스케줄 수정: scheduleId = {}, amount = {}, transferDay = {}", 
                            schedule.getId(), request.getMonthlyPaymentAmount(), request.getAutoTransferDay());
                } else {
                    // 새로운 스케줄 생성
                    AutoTransferSchedule newSchedule = AutoTransferSchedule.builder()
                            .fromAccount(fromAccount)
                            .toAccount(toAccount)
                            .amount(request.getMonthlyPaymentAmount())
                            .transferDay(request.getAutoTransferDay())
                            .active(true)
                            .deleted(false)
                            .build();
                    autoTransferScheduleRepository.save(newSchedule);
                    log.info("새 자동이체 스케줄 생성: scheduleId = {}, amount = {}, transferDay = {}", 
                            newSchedule.getId(), request.getMonthlyPaymentAmount(), request.getAutoTransferDay());
                }
            }
            
        } catch (Exception e) {
            log.warn("자동이체 설정 수정 실패: memberId = {}, toAccountId = {}, error = {}", 
                    member.getId(), toAccount.getId(), e.getMessage());
            throw e; // 자동이체 설정 수정 실패 시 전체 트랜잭션 롤백
        }
    }
    
    /**
     * 관련된 자동이체 스케줄 삭제
     */
    @Transactional
    protected void deleteRelatedAutoTransferSchedules(Member member, Account toAccount) {
        try {
            log.info("관련 자동이체 스케줄 삭제 시작: memberId = {}, toAccountId = {}", 
                    member.getId(), toAccount.getId());
            
            // 주계좌 조회 (출금 계좌)
            Account fromAccount = accountRepository.findByMemberAndAccountTypeAndDeletedFalse(
                    member, AccountType.MAIN)
                    .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
            
            // 기존 자동이체 스케줄 조회 및 삭제
            Optional<AutoTransferSchedule> existingSchedule = autoTransferScheduleRepository
                    .findByFromAccountAndToAccountAndActiveTrueAndDeletedFalse(fromAccount, toAccount);
            
            if (existingSchedule.isPresent()) {
                AutoTransferSchedule schedule = existingSchedule.get();
                schedule.setDeleted(true);
                schedule.setActive(false);
                autoTransferScheduleRepository.save(schedule);
                log.info("자동이체 스케줄 삭제 완료: scheduleId = {}", schedule.getId());
            } else {
                log.info("삭제할 자동이체 스케줄이 없습니다: memberId = {}, toAccountId = {}", 
                        member.getId(), toAccount.getId());
            }
            
        } catch (Exception e) {
            log.warn("자동이체 스케줄 삭제 실패: memberId = {}, toAccountId = {}, error = {}", 
                    member.getId(), toAccount.getId(), e.getMessage());
            // 자동이체 스케줄 삭제 실패해도 머니박스 삭제는 계속 진행
        }
    }

}
