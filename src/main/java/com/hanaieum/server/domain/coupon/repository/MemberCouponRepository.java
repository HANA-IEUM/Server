package com.hanaieum.server.domain.coupon.repository;

import com.hanaieum.server.domain.coupon.dto.CouponResponse;
import com.hanaieum.server.domain.coupon.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {
    List<MemberCoupon> findAllByMember_Id(Long memberId);
    boolean existsByCouponCode(String couponCode);
}
