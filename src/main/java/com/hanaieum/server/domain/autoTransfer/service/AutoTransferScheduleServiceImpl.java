package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.dto.TransferStatus;
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
        
        // 미래 스케줄 삭제
        Optional<AutoTransferSchedule> futureSchedule = getFutureSchedule(fromAccount, toAccount);
        if (futureSchedule.isPresent()) {
            AutoTransferSchedule schedule = futureSchedule.get();
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
    public Optional<AutoTransferSchedule> getFutureSchedule(Account fromAccount, Account toAccount) {
        LocalDate today = LocalDate.now();
        return autoTransferScheduleRepository.findFutureSchedule(fromAccount, toAccount, today);
    }
    
    /**
     * 자동이체 활성화 (내부 메서드)
     * 성능 최적화: 변경사항이 있거나 미래 스케줄이 없을 때만 DB 작업 수행
     */
    private void enableSchedule(Account fromAccount, Account toAccount, BigDecimal amount, Integer transferDay) {
        LocalDate today = LocalDate.now();
        
        // 현재 유효한 스케줄 조회
        Optional<AutoTransferSchedule> currentSchedule = getCurrentSchedule(fromAccount, toAccount);
        
        // 미래 스케줄 조회 (최대 1개)
        Optional<AutoTransferSchedule> futureSchedule = getFutureSchedule(fromAccount, toAccount);
        
        // 변경사항 체크
        boolean hasChanges = currentSchedule.isPresent() && 
            hasScheduleChanges(currentSchedule.get(), amount, transferDay);
        
        // 변경사항이 있거나 미래 스케줄이 없을 때만 처리
        if (hasChanges || futureSchedule.isEmpty()) {
            
            // 미래 스케줄 생성/업데이트
            if (futureSchedule.isPresent()) {
                // 기존 미래 스케줄 업데이트
                AutoTransferSchedule schedule = futureSchedule.get();
                schedule.setAmount(amount);
                schedule.setTransferDay(transferDay);
                autoTransferScheduleRepository.save(schedule);
                log.info("기존 미래 스케줄 업데이트: scheduleId={}, amount={}, transferDay={}", 
                        schedule.getId(), amount, transferDay);
            } else {
                // 새 스케줄 생성
                createSchedule(fromAccount, toAccount, amount, transferDay);
            }
            
            // 현재 스케줄 종료 처리 (미래 스케줄 생성)
            if (currentSchedule.isPresent()) {
                LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                currentSchedule.get().setValidTo(endOfMonth);
                autoTransferScheduleRepository.save(currentSchedule.get());
                log.info("현재 스케줄 종료 처리: scheduleId={}, validTo={}", 
                        currentSchedule.get().getId(), endOfMonth);
            }
            
        } else {
            log.info("자동이체 설정 변경사항 없음 - DB 작업 생략: fromAccountId={}, toAccountId={}", 
                    fromAccount.getId(), toAccount.getId());
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
    
    @Override
    @Transactional(readOnly = true)
    public TransferStatus getTransferStatus(Account fromAccount, Account toAccount) {
        // 현재 유효한 스케줄과 미래 스케줄 조회
        Optional<AutoTransferSchedule> currentSchedule = getCurrentSchedule(fromAccount, toAccount);
        Optional<AutoTransferSchedule> futureSchedule = getFutureSchedule(fromAccount, toAccount);
        
        // 다음달에 실제로 적용될 자동이체 정보 계산
        LocalDate nextMonth = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        
        // 현재 상태
        Boolean currentEnabled = currentSchedule.isPresent();
        BigDecimal currentAmount = currentSchedule.map(AutoTransferSchedule::getAmount).orElse(null);
        Integer currentTransferDay = currentSchedule.map(AutoTransferSchedule::getTransferDay).orElse(null);
        
        // 다음달 상태 계산
        Boolean nextEnabled;
        BigDecimal nextAmount;
        Integer nextTransferDay;
        
        if (futureSchedule.isPresent()) {
            // 미래 스케줄이 있으면 해당 스케줄이 다음달에 적용됨
            AutoTransferSchedule future = futureSchedule.get();
            nextEnabled = true;
            nextAmount = future.getAmount();
            nextTransferDay = future.getTransferDay();
        } else if (currentSchedule.isPresent()) {
            // 현재 스케줄이 있는 경우
            AutoTransferSchedule current = currentSchedule.get();

            // 현재 스케줄이 다음달에도 유효한지 확인
            boolean currentScheduleValidNextMonth = current.getValidTo() == null || 
                    !current.getValidTo().isBefore(nextMonth);
            
            if (currentScheduleValidNextMonth) {
                // 현재 스케줄이 다음달에도 계속 적용됨
                nextEnabled = true;
                nextAmount = current.getAmount();
                nextTransferDay = current.getTransferDay();
            } else {
                // 현재 스케줄이 이번달로 종료됨 -> 다음달은 비활성화
                nextEnabled = false;
                nextAmount = null;
                nextTransferDay = null;
            }
        } else {
            // 현재도 미래도 스케줄이 없으면 다음달에도 비활성화
            nextEnabled = false;
            nextAmount = null;
            nextTransferDay = null;
        }
        
        log.debug("자동이체 상태 조회 완료: fromAccountId={}, toAccountId={}, currentEnabled={}, nextEnabled={}", 
                fromAccount.getId(), toAccount.getId(), currentEnabled, nextEnabled);
        
        return TransferStatus.of(
            currentEnabled, currentAmount, currentTransferDay,
            nextEnabled, nextAmount, nextTransferDay
        );
    }
}