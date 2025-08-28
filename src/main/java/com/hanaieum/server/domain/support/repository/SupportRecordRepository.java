package com.hanaieum.server.domain.support.repository;

import com.hanaieum.server.domain.support.entity.SupportRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRecordRepository extends JpaRepository<SupportRecord, Long> {

}