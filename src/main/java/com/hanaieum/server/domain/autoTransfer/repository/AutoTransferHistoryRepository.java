package com.hanaieum.server.domain.autoTransfer.repository;

import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferHistory;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AutoTransferHistoryRepository extends JpaRepository<AutoTransferHistory, Long> {
    
    /**
     * 재시도 대상 실패한 이체 조회 (특정 날짜, 특정 재시도 횟수)
     * JOIN FETCH로 History의 fromAccount, toAccount를 미리 로딩하여 LazyInitializationException 방지
     */
    @Query("SELECT h FROM AutoTransferHistory h " +
           "JOIN FETCH h.fromAccount " +
           "JOIN FETCH h.toAccount " +
           "WHERE h.executedAt >= :startOfDay " +
           "AND h.executedAt < :endOfDay " +
           "AND h.status IN ('FAILED', 'RETRY') " +
           "AND h.retryCount = :retryCount " +
           "ORDER BY h.executedAt ASC")
    List<AutoTransferHistory> findFailedTransfersForRetry(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("retryCount") Integer retryCount
    );
    
    /**
     * 특정 스케줄의 오늘 실행 이력 존재 여부 확인
     */
    @Query("SELECT h FROM AutoTransferHistory h WHERE " +
           "h.schedule = :schedule " +
           "AND h.executedAt >= :startOfDay " +
           "AND h.executedAt < :endOfDay")
    Optional<AutoTransferHistory> findTodayExecution(
            @Param("schedule") AutoTransferSchedule schedule,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}