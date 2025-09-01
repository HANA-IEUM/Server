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
           "WHERE sr.bucketList.id = :bucketListId AND sr.deleted = false " +
           "ORDER BY sr.createdAt DESC")
    List<SupportRecord> findByBucketListIdWithDetails(@Param("bucketListId") Long bucketListId);
    
    @Query("SELECT sr FROM SupportRecord sr " +
           "JOIN FETCH sr.bucketList bl " +
           "JOIN FETCH sr.supporter s " +
           "WHERE sr.id = :supportId AND sr.deleted = false")
    java.util.Optional<SupportRecord> findByIdAndDeletedFalse(@Param("supportId") Long supportId);
}