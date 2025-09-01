package com.hanaieum.server.domain.bucketList.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BucketListRepository extends JpaRepository<BucketList, Long> {
    
    // ==================== 기본 조회 메서드 ====================
    
    /**
     * 회원별 삭제되지 않은 버킷리스트 조회 (최신순)
     */
    List<BucketList> findByMemberAndDeletedOrderByCreatedAtDesc(Member member, boolean deleted);
    
    /**
     * ID로 삭제되지 않은 버킷리스트 조회 (간단한 조회용)
     */
    Optional<BucketList> findByIdAndDeletedFalse(Long id);
    
    // ==================== 통합 조회 메서드 ====================
    
    /**
     * ID로 삭제되지 않은 버킷리스트 조회 (참여자 정보 포함)
     */
    @Query("SELECT bl FROM BucketList bl " +
           "LEFT JOIN FETCH bl.participants p " +
           "LEFT JOIN FETCH p.member " +
           "WHERE bl.id = :id AND bl.deleted = :deleted")
    Optional<BucketList> findByIdAndDeletedWithParticipants(@Param("id") Long id, @Param("deleted") boolean deleted);
    
    // ==================== 그룹 관련 조회 메서드 ====================
    
    /**
     * 그룹 내 공개 버킷리스트 조회 (최신순)
     */
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true " +
           "ORDER BY bl.createdAt DESC")
    List<BucketList> findByMemberGroupAndPublicOrderByCreatedAtDesc(@Param("group") Group group);
    
    /**
     * 그룹 내 특정 공개 버킷리스트 조회
     */
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.id = :bucketListId AND bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true")
    Optional<BucketList> findByIdAndMemberGroupAndPublic(@Param("bucketListId") Long bucketListId, @Param("group") Group group);
    
    /**
     * 특정 그룹원의 공개 버킷리스트 조회 (최신순)
     */
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.member = :member AND bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true " +
           "ORDER BY bl.createdAt DESC")
    List<BucketList> findByMemberAndMemberGroupAndPublicOrderByCreatedAtDesc(@Param("member") Member member, @Param("group") Group group);
    
    // ==================== 참여자 관련 조회 메서드 ====================
    
    /**
     * 참여중인 버킷리스트 조회 (내가 참여자로 등록된 버킷리스트들)
     */
    @Query("SELECT DISTINCT bl FROM BucketList bl " +
           "JOIN bl.participants p " +
           "WHERE p.member = :member AND p.active = true AND bl.deleted = false " +
           "ORDER BY bl.createdAt DESC")
    List<BucketList> findParticipatingBucketLists(@Param("member") Member member);
    
    // ==================== 원본 버킷리스트 관련 조회 메서드 ====================
    
    /**
     * 특정 원본 버킷리스트 ID의 모든 관련 버킷리스트 조회 (원본 포함, 생성순)
     */
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE (bl.id = :originalId OR bl.originalBucketListId = :originalId) " +
           "AND bl.deleted = false " +
           "ORDER BY bl.createdAt ASC")
    List<BucketList> findAllByOriginalBucketListId(@Param("originalId") Long originalId);
    
    // ==================== 스케줄러 관련 조회 메서드 ====================
    
    /**
     * 만료된 진행중 버킷리스트 조회 (전체)
     */
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.targetDate < :today AND bl.status = 'IN_PROGRESS' AND bl.deleted = false")
    List<BucketList> findExpiredInProgressBucketLists(@Param("today") LocalDate today);
    
    /**
     * 특정 회원의 만료된 진행중 버킷리스트 조회
     */
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.member.id = :memberId AND bl.targetDate < :today AND bl.status = 'IN_PROGRESS' AND bl.deleted = false")
    List<BucketList> findExpiredInProgressBucketListsByMember(@Param("memberId") Long memberId, @Param("today") LocalDate today);
}
