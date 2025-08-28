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
    List<BucketList> findByMemberAndDeletedOrderByCreatedAtDesc(Member member, boolean deleted);
    
    Optional<BucketList> findByIdAndDeleted(Long id, boolean deleted);
    
    // 참여자 정보와 함께 버킷리스트 조회
    @Query("SELECT bl FROM BucketList bl LEFT JOIN FETCH bl.participants p LEFT JOIN FETCH p.member WHERE bl.id = :id AND bl.deleted = :deleted")
    Optional<BucketList> findByIdAndDeletedWithParticipants(@Param("id") Long id, @Param("deleted") boolean deleted);
    
    // 그룹원들의 버킷리스트 조회 (공개된 것만)
    @Query("SELECT bl FROM BucketList bl WHERE bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true ORDER BY bl.createdAt DESC")
    List<BucketList> findByMemberGroupAndPublicOrderByCreatedAtDesc(@Param("group") Group group);
    
    // 특정 그룹원의 특정 버킷리스트 조회 (공개된 것만)
    @Query("SELECT bl FROM BucketList bl WHERE bl.id = :bucketListId AND bl.member.group = :group AND bl.deleted = false AND bl.publicFlag = true")
    Optional<BucketList> findByIdAndMemberGroupAndPublic(@Param("bucketListId") Long bucketListId, @Param("group") Group group);
}
