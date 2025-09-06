package com.hanaieum.server.domain.autoTransfer.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AutoTransferScheduleRepository extends JpaRepository<AutoTransferSchedule, Long> {
    
    /**
     * 특정 날짜에 유효한 활성 스케줄 조회 (History 방식)
     */
    @Query("SELECT ats FROM AutoTransferSchedule ats WHERE " +
           "ats.fromAccount = :fromAccount AND ats.toAccount = :toAccount AND " +
           "ats.validFrom <= :date AND (ats.validTo IS NULL OR ats.validTo >= :date) AND " +
           "ats.active = true AND ats.deleted = false")
    Optional<AutoTransferSchedule> findActiveSchedule(
        @Param("fromAccount") Account fromAccount,
        @Param("toAccount") Account toAccount, 
        @Param("date") LocalDate date);
    
    /**
     * 특정 날짜 이후 시작되는 미래 스케줄 조회
     */
    @Query("SELECT ats FROM AutoTransferSchedule ats WHERE " +
           "ats.fromAccount = :fromAccount AND ats.toAccount = :toAccount AND " +
           "ats.validFrom > :date AND ats.active = true AND ats.deleted = false " +
           "ORDER BY ats.validFrom ASC")
    List<AutoTransferSchedule> findFutureSchedules(
        @Param("fromAccount") Account fromAccount,
        @Param("toAccount") Account toAccount,
        @Param("date") LocalDate date);
    
    /**
     * 머니박스 계좌를 목적지로 하는 모든 자동이체 스케줄 조회 (삭제되지 않은 것만)
     * - 머니박스 삭제 시 관련된 모든 스케줄 삭제를 위해 사용
     */
    @Query("SELECT ats FROM AutoTransferSchedule ats WHERE " +
           "ats.toAccount = :moneyBoxAccount AND ats.deleted = false")
    List<AutoTransferSchedule> findAllByToAccountAndDeletedFalse(
        @Param("moneyBoxAccount") Account moneyBoxAccount);

    /**
     * 특정 이체일에 실행될 자동이체 스케줄 조회 (배치 처리용)
     * 오늘 날짜의 "이체일"과 일치하는 스케줄
     * 스케줄이 시작일(validFrom) 이후이며, 종료일(validTo)이 없거나 아직 종료되지 않은 스케줄
     * 활성화되었으며 삭제 처리되지 않은 자동이체 스케줄
     */
    @Query("SELECT ats FROM AutoTransferSchedule ats WHERE " +
           "ats.transferDay = :targetDay AND " +
           "ats.validFrom <= :targetDate AND " +
           "(ats.validTo IS NULL OR ats.validTo >= :targetDate) AND " +
           "ats.active = true AND ats.deleted = false")
    List<AutoTransferSchedule> findSchedulesForExecution(
        @Param("targetDate") LocalDate targetDate,
        @Param("targetDay") Integer targetDay
    );
}