package com.hanaieum.server.domain.bucketList.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BucketListRepository extends JpaRepository<BucketList, Long> {
    List<BucketList> findByMemberAndDeletedOrderByCreatedAtDesc(Member member, boolean deleted);
}
