package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferHistory;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;

import java.time.LocalDate;
import java.util.List;

public interface AutoTransferService {
    
    /**
     * 특정 날짜에 실행해야 하는 자동이체 처리
     */
    void executeScheduledTransfers(LocalDate targetDate);
    
    /**
     * 단일 스케줄 실행
     */
    AutoTransferHistory executeTransfer(AutoTransferSchedule schedule);
    
    /**
     * 특정 날짜 + 재시도 횟수의 실패한 자동이체 재시도 처리
     */
    void retryFailedTransfers(LocalDate targetDate, Integer retryCount);
    
    /**
     * 특정 날짜의 실행 대상 스케줄 조회
     */
    List<AutoTransferSchedule> getSchedulesToExecute(LocalDate targetDate);
}