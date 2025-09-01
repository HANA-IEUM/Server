package com.hanaieum.server.domain.coupon.service;

import com.hanaieum.server.domain.coupon.dto.CouponResponse;

import java.util.List;

public interface CouponService {
    void createMemberCoupon(Long bucketId);
    List<CouponResponse> getCoupons(Long memberId);
}
