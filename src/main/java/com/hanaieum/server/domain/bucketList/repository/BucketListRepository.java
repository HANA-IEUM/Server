package com.hanaieum.server.domain.bucketList.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
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

    // 상태별 버킷리스트 조회
    List<BucketList> findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(Member member, BucketListStatus status, boolean deleted);

    Optional<BucketList> findByIdAndDeleted(Long id, boolean deleted);

    // 참여자 정보와 함께 버킷리스트 조회
    @Query("SELECT bl FROM BucketList bl LEFT JOIN FETCH bl.participants p LEFT JOIN FETCH p.member WHERE bl.id = :id AND bl.deleted = :deleted")
    Optional<BucketList> findByIdAndDeletedWithParticipants(@Param("id") Long id, @Param("deleted") boolean deleted);

    // 그룹원의 진행중인 버킷리스트 목록 조회 (공개된 것만)
    @Query("SELECT bl FROM BucketList bl WHERE bl.member.id = :groupMemberId AND bl.status = 'IN_PROGRESS' AND bl.deleted = false AND bl.publicFlag = true ORDER BY bl.createdAt DESC")
    List<BucketList> findByGroupMemberIdAndInProgressAndPublic(@Param("groupMemberId") Long groupMemberId);

    // 그룹원의 완료된 버킷리스트 목록 조회 (공개된 것만)
    @Query("SELECT bl FROM BucketList bl WHERE bl.member.id = :groupMemberId AND bl.status = 'COMPLETED' AND bl.deleted = false AND bl.publicFlag = true ORDER BY bl.createdAt DESC")
    List<BucketList> findByGroupMemberIdAndCompletedAndPublic(@Param("groupMemberId") Long groupMemberId);

    // 참여중인 버킷리스트 조회 (참여자 테이블을 통해)
    @Query("SELECT DISTINCT bl FROM BucketList bl " +
            "JOIN bl.participants p " +
            "WHERE p.member.id = :memberId " +
            "AND p.active = true " +
            "AND bl.deleted = :deleted " +
            "ORDER BY bl.createdAt DESC")
    List<BucketList> findParticipatedBucketListsByMember(@Param("memberId") Long memberId, @Param("deleted") boolean deleted);

    // Transfer Service용 - ID로 삭제되지 않은 버킷리스트 조회
    Optional<BucketList> findByIdAndDeletedFalse(Long id);
}
