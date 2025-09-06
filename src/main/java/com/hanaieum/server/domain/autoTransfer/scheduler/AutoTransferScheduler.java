package com.hanaieum.server.domain.autoTransfer.scheduler;

import com.hanaieum.server.domain.autoTransfer.service.AutoTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 자동이체 스케줄러
 * - 9시: 정규 출금 시도
 * - 12시: 1차 재시도
 * - 15시: 2차 재시도  
 * - 다음날 9시: 3차 재시도 (최종)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoTransferScheduler {
    
    private final AutoTransferService autoTransferService;
    
    /**
     * 매일 오전 9시: 정규 자동이체 실행 + 어제 실패분 최종 재시도
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Seoul")
    public void executeAt9AM() {
        LocalDate today = LocalDate.now();
        log.info("오전 9시 자동이체 실행 시작: {}", today);
        
        try {
            // 1. 정규 자동이체 실행
            autoTransferService.executeScheduledTransfers(today);

            // 2. 어제 실패분 최종 재시도 (retryCount = 2)
            LocalDate yesterday = today.minusDays(1);
            autoTransferService.retryFailedTransfers(yesterday, 2);
            
            log.info("오전 9시 자동이체 실행 완료: {}", today);
        } catch (Exception e) {
            log.error("오전 9시 자동이체 실행 중 예외 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 매일 오후 12시: 당일 실패분 1차 재시도
     */
    @Scheduled(cron = "0 0 12 * * ?", zone = "Asia/Seoul")
    public void retryAt12PM() {
        LocalDate today = LocalDate.now();
        log.info("오후 12시 1차 재시도 시작: {}", today);
        
        try {
            autoTransferService.retryFailedTransfers(today, 0);
            log.info("오후 12시 1차 재시도 완료: {}", today);
        } catch (Exception e) {
            log.error("오후 12시 1차 재시도 중 예외 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 매일 오후 3시: 당일 실패분 2차 재시도
     */
    @Scheduled(cron = "0 0 15 * * ?", zone = "Asia/Seoul")
    public void retryAt3PM() {
        LocalDate today = LocalDate.now();
        log.info("오후 3시 2차 재시도 시작: {}", today);
        
        try {
            autoTransferService.retryFailedTransfers(today, 1);
            log.info("오후 3시 2차 재시도 완료: {}", today);
        } catch (Exception e) {
            log.error("오후 3시 2차 재시도 중 예외 발생: {}", e.getMessage(), e);
        }
    }
}