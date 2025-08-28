package com.hanaieum.server.domain.autoTransfer.repository;

import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoTransferScheduleRepository extends JpaRepository<AutoTransferSchedule, Long> {

}