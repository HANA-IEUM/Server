package com.hanaieum.server.domain.autoTransfer.service;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AutoTransferScheduleService {

    /**
     * 자동이체 스케줄 생성 (다음달부터 시작)
     */
    AutoTransferSchedule createSchedule(Account fromAccount, Account toAccount, 
                                      BigDecimal amount, Integer transferDay);

    /**
     * 자동이체 스케줄 업데이트 (History 방식)
     * - 기존 미래 스케줄이 있으면 업데이트
     * - 없으면 새로 생성
     * - 현재 설정과 다르면 현재 스케줄 종료 처리
     */
    void updateSchedule(Account fromAccount, Account toAccount,
                       Boolean enabled, BigDecimal amount, Integer transferDay);

    /**
     * 자동이체 비활성화
     * - 현재 스케줄 종료 처리
     * - 미래 스케줄들 삭제
     */
    void disableSchedule(Account fromAccount, Account toAccount);

    /**
     * 현재 유효한 스케줄 조회
     */
    Optional<AutoTransferSchedule> getCurrentSchedule(Account fromAccount, Account toAccount);

    /**
     * 미래 스케줄들 조회
     */
    List<AutoTransferSchedule> getFutureSchedules(Account fromAccount, Account toAccount);

    /**
     * 특정 날짜에 유효한 스케줄 조회
     */
    Optional<AutoTransferSchedule> getScheduleAt(Account fromAccount, Account toAccount, LocalDate date);
    
    /**
     * 스케줄 존재 여부 확인
     */
    boolean hasActiveSchedule(Account fromAccount, Account toAccount);
}