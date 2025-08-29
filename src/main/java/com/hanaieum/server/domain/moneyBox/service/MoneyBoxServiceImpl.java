package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferScheduleRepository;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;
import com.hanaieum.server.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyBoxServiceImpl implements MoneyBoxService {

    private final AccountRepository accountRepository;
    private final AutoTransferScheduleRepository autoTransferScheduleRepository;

    @Override
    public MoneyBoxResponse updateMoneyBoxName(Long accountId, MoneyBoxRequest request) {
        Member currentMember = getCurrentMember();
        
        // 계좌 조회 및 검증
        Account account = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 계좌 소유권 검증
        if (!account.getMember().getId().equals(currentMember.getId())) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
        
        // MONEY_BOX 타입 계좌인지 검증
        if (account.getAccountType() != AccountType.MONEY_BOX) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        
        // 머니박스 별명 업데이트
        account.setBoxName(request.getBoxName());
        
        // 자동이체 정보 업데이트 (값이 제공된 경우)
        if (request.getMonthlyAmount() != null || request.getTransferDay() != null) {
            updateAutoTransferSchedule(account, currentMember, request.getMonthlyAmount(), request.getTransferDay());
        }
        
        Account savedAccount = accountRepository.save(account);
        
        log.info("머니박스 수정 완료: accountId={}, newBoxName={}, monthlyAmount={}, transferDay={}", 
                accountId, request.getBoxName(), request.getMonthlyAmount(), request.getTransferDay());
        
        return MoneyBoxResponse.of(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyBoxResponse> getMyMoneyBoxList() {
        Member currentMember = getCurrentMember();
        
        // 사용자의 모든 MONEY_BOX 타입 계좌 조회
        List<Account> moneyBoxAccounts = accountRepository.findAllByMemberAndAccountTypeAndDeletedFalse(
                currentMember, AccountType.MONEY_BOX);
        
        // 삭제된 버킷리스트와 연결된 머니박스는 제외
        List<Account> activeMoneyBoxAccounts = moneyBoxAccounts.stream()
                .filter(account -> account.getBucketList() == null || !account.getBucketList().isDeleted())
                .toList();
        
        log.info("머니박스 목록 조회 완료: memberId={}, totalCount={}, activeCount={}", 
                currentMember.getId(), moneyBoxAccounts.size(), activeMoneyBoxAccounts.size());
        
        return activeMoneyBoxAccounts.stream()
                .map(MoneyBoxResponse::of)
                .toList();
    }

    private Member getCurrentMember() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getMember();
    }
    
    /**
     * 자동이체 스케줄 업데이트
     */
    private void updateAutoTransferSchedule(Account moneyBoxAccount, Member member, 
                                          java.math.BigDecimal monthlyAmount, Integer transferDay) {
        try {
            // 사용자의 주계좌 조회 (출금 계좌)
            Account mainAccount = accountRepository.findByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN)
                    .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
            
            // 기존 자동이체 스케줄 조회
            Optional<AutoTransferSchedule> existingSchedule = autoTransferScheduleRepository
                    .findByFromAccountAndToAccountAndActiveTrueAndDeletedFalse(mainAccount, moneyBoxAccount);
            
            if (existingSchedule.isPresent()) {
                // 기존 스케줄 업데이트
                AutoTransferSchedule schedule = existingSchedule.get();
                
                if (monthlyAmount != null) {
                    schedule.setAmount(monthlyAmount);
                    log.info("자동이체 금액 수정: accountId={}, newAmount={}", moneyBoxAccount.getId(), monthlyAmount);
                }
                
                if (transferDay != null) {
                    schedule.setTransferDay(transferDay);
                    log.info("자동이체 날짜 수정: accountId={}, newTransferDay={}일", moneyBoxAccount.getId(), transferDay);
                }
                
                autoTransferScheduleRepository.save(schedule);
                log.info("자동이체 스케줄 업데이트 완료: scheduleId={}", schedule.getId());
                
            } else if (monthlyAmount != null && transferDay != null) {
                // 기존 스케줄이 없고 두 값이 모두 제공된 경우 새로 생성
                AutoTransferSchedule newSchedule = AutoTransferSchedule.builder()
                        .fromAccount(mainAccount)
                        .toAccount(moneyBoxAccount)
                        .amount(monthlyAmount)
                        .transferDay(transferDay)
                        .active(true)
                        .deleted(false)
                        .build();
                
                autoTransferScheduleRepository.save(newSchedule);
                log.info("새 자동이체 스케줄 생성 완료: accountId={}, monthlyAmount={}, transferDay={}일", 
                        moneyBoxAccount.getId(), monthlyAmount, transferDay);
            } else {
                log.warn("자동이체 스케줄이 없고 필수 정보가 부족하여 생성하지 않음: accountId={}", moneyBoxAccount.getId());
            }
            
        } catch (Exception e) {
            log.warn("자동이체 스케줄 업데이트 실패: accountId={}, error={}", moneyBoxAccount.getId(), e.getMessage());
            // 자동이체 업데이트 실패해도 머니박스 수정은 성공으로 처리
        }
    }
}