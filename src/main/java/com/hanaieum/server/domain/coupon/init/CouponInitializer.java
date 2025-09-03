package com.hanaieum.server.domain.coupon.init;

import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.coupon.entity.Coupon;
import com.hanaieum.server.domain.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponInitializer implements CommandLineRunner {

    private final CouponRepository couponRepository;

    @Override
    public void run(String... args) {

        // 데이터가 있으면 초기화하지 않음
        if (couponRepository.count() > 0) {
            return;
        }

        // 카테고리별 초기 쿠폰 리스트
        List<Coupon> initialCoupons = List.of(
                // TRIP
                Coupon.builder().category(BucketListType.TRIP).partnerName("여행 제휴사1").couponName("Trip Coupon 1").description("여행 쿠폰 설명1").validityDays(365).build(),
                Coupon.builder().category(BucketListType.TRIP).partnerName("여행 제휴사2").couponName("Trip Coupon 2").description("여행 쿠폰 설명2").validityDays(365).build(),
                Coupon.builder().category(BucketListType.TRIP).partnerName("여행 제휴사3").couponName("Trip Coupon 3").description("여행 쿠폰 설명3").validityDays(365).build(),

                // HOBBY
                Coupon.builder().category(BucketListType.HOBBY).partnerName("취미 제휴사1").couponName("Hobby Coupon 1").description("취미 쿠폰 설명1").validityDays(365).build(),
                Coupon.builder().category(BucketListType.HOBBY).partnerName("취미 제휴사2").couponName("Hobby Coupon 2").description("취미 쿠폰 설명2").validityDays(365).build(),
                Coupon.builder().category(BucketListType.HOBBY).partnerName("취미 제휴사3").couponName("Hobby Coupon 3").description("취미 쿠폰 설명3").validityDays(365).build(),

                // HEALTH
                Coupon.builder().category(BucketListType.HEALTH).partnerName("건강 제휴사1").couponName("Health Coupon 1").description("건강 쿠폰 설명 1").validityDays(365).build(),
                Coupon.builder().category(BucketListType.HEALTH).partnerName("건강 제휴사2").couponName("Health Coupon 2").description("건강 쿠폰 설명 2").validityDays(365).build(),
                Coupon.builder().category(BucketListType.HEALTH).partnerName("건강 제휴사3").couponName("Health Coupon 3").description("건강 쿠폰 설명 3").validityDays(365).build(),

                // FAMILY
                Coupon.builder().category(BucketListType.FAMILY).partnerName("가족 제휴사1").couponName("Family Coupon 1").description("가족 쿠폰 설명 1").validityDays(365).build(),
                Coupon.builder().category(BucketListType.FAMILY).partnerName("가족 제휴사2").couponName("Family Coupon 2").description("가족 쿠폰 설명 2").validityDays(365).build(),
                Coupon.builder().category(BucketListType.FAMILY).partnerName("가족 제휴사3").couponName("Family Coupon 3").description("가족 쿠폰 설명 3").validityDays(365).build()
        );

        couponRepository.saveAll(initialCoupons);
    }
}
