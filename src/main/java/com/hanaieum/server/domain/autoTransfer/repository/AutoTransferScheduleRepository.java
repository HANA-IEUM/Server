package com.hanaieum.server.domain.autoTransfer.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutoTransferScheduleRepository extends JpaRepository<AutoTransferSchedule, Long> {
    
    /**
     * 출금계좌와 입금계좌로 자동이체 스케줄 조회
     */
    Optional<AutoTransferSchedule> findByFromAccountAndToAccountAndActiveTrueAndDeletedFalse(
            Account fromAccount, Account toAccount);
    
    /**
     * 특정 이체일에 실행될 자동이체 스케줄 조회
     */
    @Query("SELECT ats FROM AutoTransferSchedule ats WHERE ats.transferDay = :transferDay AND ats.active = true AND ats.deleted = false")
    List<AutoTransferSchedule> findByTransferDayAndActive(@Param("transferDay") Integer transferDay);
}