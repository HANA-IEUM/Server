package com.hanaieum.server.domain.bucketList.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BucketListRepository extends JpaRepository<BucketList, Long> {
    
    // 기본 조회 메서드들
    List<BucketList> findByMemberAndDeletedOrderByCreatedAtDesc(Member member, boolean deleted);
    
    // 통합된 버킷리스트 조회 메서드 (참여자 정보 포함, 기본 사용)
    @Query("SELECT bl FROM BucketList bl " +
           "LEFT JOIN FETCH bl.participants p " +
           "LEFT JOIN FETCH p.member " +
           "WHERE bl.id = :id AND bl.deleted = :deleted")
    Optional<BucketList> findByIdAndDeletedWithParticipants(@Param("id") Long id, @Param("deleted") boolean deleted);
    
    // 간단한 조회용 (참여자 정보 불필요한 경우)
    Optional<BucketList> findByIdAndDeletedFalse(Long id);
    
    // 그룹 관련 조회 메서드들
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true " +
           "ORDER BY bl.createdAt DESC")
    List<BucketList> findByMemberGroupAndPublicOrderByCreatedAtDesc(@Param("group") Group group);
    
    @Query("SELECT bl FROM BucketList bl " +
           "WHERE bl.id = :bucketListId AND bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true")
    Optional<BucketList> findByIdAndMemberGroupAndPublic(@Param("bucketListId") Long bucketListId, @Param("group") Group group);
    
    // 참여자 관련 조회
    @Query("SELECT DISTINCT bl FROM BucketList bl " +
           "JOIN bl.participants p " +
           "WHERE p.member = :member AND p.active = true AND bl.deleted = false " +
           "ORDER BY bl.createdAt DESC")
    List<BucketList> findByParticipantMemberAndActiveOrderByCreatedAtDesc(@Param("member") Member member);
}
