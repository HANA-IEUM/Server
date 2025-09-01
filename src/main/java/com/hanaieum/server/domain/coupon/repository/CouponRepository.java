package com.hanaieum.server.domain.coupon.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findAllByCategory(BucketListType category);
}
