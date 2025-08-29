package com.hanaieum.server.domain.bucketList.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketParticipant;
import com.hanaieum.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BucketParticipantRepository extends JpaRepository<BucketParticipant, Long> {
    
    List<BucketParticipant> findByBucketListAndIsActiveTrue(BucketList bucketList);
    
    List<BucketParticipant> findByMemberAndIsActiveTrue(Member member);
    
    boolean existsByBucketListAndMemberAndIsActiveTrue(BucketList bucketList, Member member);
    
    // 수정 기능을 위한 추가 메서드들
    List<BucketParticipant> findByBucketListAndIsActive(BucketList bucketList, Boolean isActive);
    
    Optional<BucketParticipant> findByBucketListAndMember(BucketList bucketList, Member member);
}
