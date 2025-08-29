package com.hanaieum.server.domain.support.repository;

import com.hanaieum.server.domain.support.entity.SupportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportRecordRepository extends JpaRepository<SupportRecord, Long> {

    @Query("SELECT sr FROM SupportRecord sr " +
           "JOIN FETCH sr.bucketList bl " +
           "JOIN FETCH sr.supporter s " +
           "WHERE sr.bucketList.id = :bucketListId " +
           "ORDER BY sr.createdAt DESC")
    List<SupportRecord> findByBucketListIdWithDetails(@Param("bucketListId") Long bucketListId);
}