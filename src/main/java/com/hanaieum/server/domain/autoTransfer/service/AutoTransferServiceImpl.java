package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferHistory;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferStatus;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferHistoryRepository;
import com.hanaieum.server.domain.autoTransfer.repository.AutoTransferScheduleRepository;
import com.hanaieum.server.domain.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoTransferServiceImpl implements AutoTransferService {
    
    private final AutoTransferScheduleRepository scheduleRepository;
    private final AutoTransferHistoryRepository historyRepository;
    private final TransferService transferService;
    
    @Override
    public void executeScheduledTransfers(LocalDate targetDate) {
        log.info("자동이체 실행 시작: targetDate={}", targetDate);
        
        List<AutoTransferSchedule> schedules = getSchedulesToExecute(targetDate);
        log.info("실행 대상 스케줄 개수: {}", schedules.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (AutoTransferSchedule schedule : schedules) {
            try {
                // 오늘 이미 실행된 스케줄인지 확인
                if (isAlreadyExecutedToday(schedule, targetDate)) {
                    log.info("스케줄 {}은 이미 오늘 실행되었습니다", schedule.getId());
                    continue;
                }
                
                AutoTransferHistory history = executeTransfer(schedule);
                if (history.getStatus() == AutoTransferStatus.SUCCESS) {
                    successCount++;
                } else {
                    failureCount++;
                }
                
            } catch (Exception e) {
                log.error("자동이체 실행 중 예외 발생: scheduleId={}, error={}",
                         schedule.getId(), e.getMessage(), e);
                failureCount++;
            }
        }
        
        log.info("자동이체 실행 완료: 성공={}, 실패={}", successCount, failureCount);
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AutoTransferHistory executeTransfer(AutoTransferSchedule schedule) {
        log.info("자동이체 실행: scheduleId={}, fromAccountId={}, toAccountId={}, amount={}", 
                schedule.getId(), schedule.getFromAccount().getId(), 
                schedule.getToAccount().getId(), schedule.getAmount());
        
        LocalDateTime executionTime = LocalDateTime.now();
        
        try {
            // TransferService를 통한 실제 이체 실행
            executeActualTransfer(schedule);
            
            // 성공 이력 기록
            AutoTransferHistory history = AutoTransferHistory.builder()
                    .schedule(schedule)
                    .fromAccount(schedule.getFromAccount())
                    .toAccount(schedule.getToAccount())
                    .amount(schedule.getAmount())
                    .executedAt(executionTime)
                    .status(AutoTransferStatus.SUCCESS)
                    .failureReason(null)
                    .retryCount(0)
                    .build();
            
            AutoTransferHistory savedHistory = historyRepository.save(history);
            log.info("자동이체 성공: historyId={}", savedHistory.getId());
            
            return savedHistory;
            
        } catch (Exception e) {
            log.error("자동이체 실패: scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
            
            // 실패 이력 기록
            AutoTransferHistory history = AutoTransferHistory.builder()
                    .schedule(schedule)
                    .fromAccount(schedule.getFromAccount())
                    .toAccount(schedule.getToAccount())
                    .amount(schedule.getAmount())
                    .executedAt(executionTime)
                    .status(AutoTransferStatus.FAILED)
                    .failureReason(e.getMessage())
                    .retryCount(0)
                    .build();
            
            return historyRepository.save(history);
        }
    }
    
    @Override
    public void retryFailedTransfers(LocalDate targetDate, Integer retryCount) {
        log.info("자동이체 재시도 시작 - 날짜: {}, 재시도 횟수: {}", targetDate, retryCount);
        
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        
        List<AutoTransferHistory> failedTransfers = 
            historyRepository.findFailedTransfersForRetry(startOfDay, endOfDay, retryCount);
        
        log.info("재시도 대상: {}건", failedTransfers.size());
        
        int successCount = 0;
        int failedCount = 0;
        
        for (AutoTransferHistory history : failedTransfers) {
            try {
                retryOneHistory(history);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("자동이체 재시도 처리 중 예외 발생: historyId={}, error={}", 
                         history.getId(), e.getMessage(), e);
            }
        }
        
        log.info("자동이체 재시도 완료 - 날짜: {}, 성공: {}건, 실패: {}건", targetDate, successCount, failedCount);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AutoTransferSchedule> getSchedulesToExecute(LocalDate targetDate) {
        int targetDay = targetDate.getDayOfMonth();
        return scheduleRepository.findSchedulesForExecution(targetDate, targetDay);
    }
    
    /**
     * 개별 재시도 처리 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void retryOneHistory(AutoTransferHistory history) {
        log.debug("자동이체 재시도 - History ID: {}, Schedule ID: {}, Retry Count: {}", 
                 history.getId(), history.getSchedule().getId(), history.getRetryCount());
        
        try {
            // 이체 재실행 - History 기반 오버로딩 메서드 사용
            executeActualTransfer(history);
            
            // 성공 처리
            history.updateStatus(AutoTransferStatus.SUCCESS, null);
            historyRepository.save(history);
            log.info("자동이체 재시도 성공 - History ID: {}", history.getId());
            
        } catch (Exception e) {
            // 재시도 횟수 증가
            history.incrementRetryCount();
            
            // 최종 실패 여부 판단 (3차 재시도 후 실패)
            if (history.getRetryCount() >= 3) {
                history.updateStatus(AutoTransferStatus.FAILED, e.getMessage());
                log.warn("자동이체 최종 실패 - History ID: {}, 사유: {}", 
                        history.getId(), e.getMessage());
            } else {
                history.updateStatus(AutoTransferStatus.RETRY, e.getMessage());
                log.warn("자동이체 재시도 실패 - History ID: {}, Retry Count: {}, 사유: {}", 
                        history.getId(), history.getRetryCount(), e.getMessage());
            }

            historyRepository.save(history);
            
            // 실패 시에도 예외를 다시 던져서 상위에서 실패 카운트 처리
            throw e;
        }
    }
    
    /**
     * 실제 이체 실행 로직 - Schedule 기반 (정규 실행용)
     */
    private void executeActualTransfer(AutoTransferSchedule schedule) {
        transferService.executeAutoTransfer(
            schedule.getFromAccount().getId(),
            schedule.getToAccount().getId(),
            schedule.getAmount(),
            schedule.getId()
        );
    }
    
    /**
     * 실제 이체 실행 로직 - History 기반 (재시도용)
     */
    private void executeActualTransfer(AutoTransferHistory history) {
        transferService.executeAutoTransfer(
            history.getFromAccount().getId(),
            history.getToAccount().getId(),
            history.getAmount(),
            history.getSchedule().getId()
        );
    }
    
    /**
     * 오늘 이미 실행된 스케줄인지 확인
     */
    private boolean isAlreadyExecutedToday(AutoTransferSchedule schedule, LocalDate targetDate) {
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        
        Optional<AutoTransferHistory> todayExecution = 
            historyRepository.findTodayExecution(schedule, startOfDay, endOfDay);
        
        return todayExecution.isPresent() && 
               todayExecution.get().getStatus() == AutoTransferStatus.SUCCESS;
    }
}