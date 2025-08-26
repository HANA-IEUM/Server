package com.hanaieum.server.domain.bucketList.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BucketListRepository extends JpaRepository<BucketList, Long> {
}
