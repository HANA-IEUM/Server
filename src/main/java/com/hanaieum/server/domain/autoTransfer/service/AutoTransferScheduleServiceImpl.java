package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AutoTransferScheduleServiceImpl implements AutoTransferScheduleService {
    
    private final AutoTransferScheduleRepository autoTransferScheduleRepository;
    
    @Override
    public AutoTransferSchedule createSchedule(Account fromAccount, Account toAccount, 
                                             BigDecimal amount, Integer transferDay) {
        LocalDate nextMonth = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        
        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .transferDay(transferDay)
                .validFrom(nextMonth)
                .validTo(null)
                .active(true)
                .deleted(false)
                .build();
                
        AutoTransferSchedule saved = autoTransferScheduleRepository.save(schedule);
        
        log.info("자동이체 스케줄 생성: fromAccountId={}, toAccountId={}, validFrom={}, amount={}, transferDay={}", 
                fromAccount.getId(), toAccount.getId(), nextMonth, amount, transferDay);
                
        return saved;
    }
    
    @Override
    public void updateSchedule(Account fromAccount, Account toAccount,
                              Boolean enabled, BigDecimal amount, Integer transferDay) {
        if (enabled) {
            enableSchedule(fromAccount, toAccount, amount, transferDay);
        } else {
            disableSchedule(fromAccount, toAccount);
        }
    }
    
    @Override
    public void disableSchedule(Account fromAccount, Account toAccount) {
        LocalDate today = LocalDate.now();
        
        // 현재 유효한 스케줄 종료 처리
        Optional<AutoTransferSchedule> currentSchedule = getCurrentSchedule(fromAccount, toAccount);
        if (currentSchedule.isPresent()) {
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            currentSchedule.get().setValidTo(endOfMonth);
            autoTransferScheduleRepository.save(currentSchedule.get());
            log.info("현재 스케줄 비활성화: scheduleId={}, validTo={}", 
                    currentSchedule.get().getId(), endOfMonth);
        }
        
        // 미래 스케줄들 삭제
        List<AutoTransferSchedule> futureSchedules = getFutureSchedules(fromAccount, toAccount);
        for (AutoTransferSchedule schedule : futureSchedules) {
            schedule.setDeleted(true);
            autoTransferScheduleRepository.save(schedule);
            log.info("미래 스케줄 삭제: scheduleId={}", schedule.getId());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<AutoTransferSchedule> getCurrentSchedule(Account fromAccount, Account toAccount) {
        LocalDate today = LocalDate.now();
        return autoTransferScheduleRepository.findActiveSchedule(fromAccount, toAccount, today);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AutoTransferSchedule> getFutureSchedules(Account fromAccount, Account toAccount) {
        LocalDate today = LocalDate.now();
        return autoTransferScheduleRepository.findFutureSchedules(fromAccount, toAccount, today);
    }
    
    /**
     * 자동이체 활성화 (내부 메서드)
     */
    private void enableSchedule(Account fromAccount, Account toAccount, BigDecimal amount, Integer transferDay) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.withDayOfMonth(1).plusMonths(1);
        
        // 현재 유효한 스케줄 조회
        Optional<AutoTransferSchedule> currentSchedule = getCurrentSchedule(fromAccount, toAccount);
        
        // 미래 스케줄들 조회
        List<AutoTransferSchedule> futureSchedules = getFutureSchedules(fromAccount, toAccount);
        
        if (!futureSchedules.isEmpty()) {
            // 기존 미래 스케줄 업데이트 (중복 생성 방지)
            AutoTransferSchedule schedule = futureSchedules.get(0);
            schedule.setAmount(amount);
            schedule.setTransferDay(transferDay);
            autoTransferScheduleRepository.save(schedule);
            log.info("기존 미래 스케줄 업데이트: scheduleId={}, amount={}, transferDay={}", 
                    schedule.getId(), amount, transferDay);
        } else {
            // 새 스케줄 생성 (미래 스케줄이 없을 때만)
            createSchedule(fromAccount, toAccount, amount, transferDay);
        }
        
        // 현재 스케줄 종료 처리 (설정이 다르다면)
        if (currentSchedule.isPresent() && hasScheduleChanges(currentSchedule.get(), amount, transferDay)) {
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            currentSchedule.get().setValidTo(endOfMonth);
            autoTransferScheduleRepository.save(currentSchedule.get());
            log.info("현재 스케줄 종료 처리: scheduleId={}, validTo={}", 
                    currentSchedule.get().getId(), endOfMonth);
        }
    }
    
    /**
     * 스케줄 변경 여부 확인
     */
    private boolean hasScheduleChanges(AutoTransferSchedule currentSchedule, BigDecimal amount, Integer transferDay) {
        return !currentSchedule.getAmount().equals(amount) ||
               !currentSchedule.getTransferDay().equals(transferDay);
    }
    
    @Override
    public void deleteAllSchedulesForMoneyBox(Account moneyBoxAccount) {
        log.info("머니박스 삭제에 따른 모든 자동이체 스케줄 삭제 시작: moneyBoxAccountId={}", moneyBoxAccount.getId());
        
        // 머니박스를 목적지로 하는 모든 스케줄 조회 (삭제되지 않은 것만)
        List<AutoTransferSchedule> allSchedules = autoTransferScheduleRepository
                .findAllByToAccountAndDeletedFalse(moneyBoxAccount);
        
        if (allSchedules.isEmpty()) {
            log.info("삭제할 자동이체 스케줄이 없음: moneyBoxAccountId={}", moneyBoxAccount.getId());
            return;
        }
        
        // 모든 스케줄을 삭제 처리 (Soft Delete)
        for (AutoTransferSchedule schedule : allSchedules) {
            schedule.setDeleted(true);
            schedule.setActive(false); // 활성화도 함께 해제
            
            log.debug("자동이체 스케줄 삭제: scheduleId={}, fromAccountId={}, toAccountId={}, validFrom={}", 
                    schedule.getId(), schedule.getFromAccount().getId(), 
                    schedule.getToAccount().getId(), schedule.getValidFrom());
        }
        
        autoTransferScheduleRepository.saveAll(allSchedules);
        
        log.info("머니박스 삭제에 따른 모든 자동이체 스케줄 삭제 완료: moneyBoxAccountId={}, 삭제된 스케줄 수={}", 
                moneyBoxAccount.getId(), allSchedules.size());
    }
}