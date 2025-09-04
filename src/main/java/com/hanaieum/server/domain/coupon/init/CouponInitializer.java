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
                Coupon.builder().category(BucketListType.TRIP).partnerName("대한항공").couponName("항공권 할인쿠폰").description("최대 할인금액 : 300,000원").validityDays(180).build(),
                Coupon.builder().category(BucketListType.TRIP).partnerName("하나투어").couponName("숙박 할인쿠폰").description("최대 할인금액 : 200,000원").validityDays(180).build(),
                Coupon.builder().category(BucketListType.TRIP).partnerName("신세계면세점").couponName("온/오프라인 할인쿠폰").description("최대 할인금액 : 200,000원").validityDays(30).build(),

                // HOBBY
                Coupon.builder().category(BucketListType.HOBBY).partnerName("하비풀").couponName("클래스 할인쿠폰").description("최대 할인금액 : 50,000원").validityDays(90).build(),
                Coupon.builder().category(BucketListType.HOBBY).partnerName("교보문고").couponName("도서 할인쿠폰").description("최대 할인금액 : 50,000원").validityDays(90).build(),
                Coupon.builder().category(BucketListType.HOBBY).partnerName("인터파크").couponName("공연 할인쿠폰").description("최대 할인금액 : 100,000원").validityDays(90).build(),

                // HEALTH
                Coupon.builder().category(BucketListType.HEALTH).partnerName("하나로 의료재단").couponName("종합건강검진 할인쿠폰").description("최대 할인금액 : 500,000원").validityDays(365).build(),
                Coupon.builder().category(BucketListType.HEALTH).partnerName("하나의료기기").couponName("대여/구매 할인쿠폰").description("최대 할인금액 : 200,000원").validityDays(90).build(),
                Coupon.builder().category(BucketListType.HEALTH).partnerName("하나손해보험").couponName("첫 달 보험료 할인쿠폰").description("최대 할인금액 : 100,000원").validityDays(90).build(),

                // FAMILY
                Coupon.builder().category(BucketListType.FAMILY).partnerName("펫포레스트").couponName("반려동물 장례 할인쿠폰").description("최대 할인금액 : 300,000원").validityDays(365).build(),
                Coupon.builder().category(BucketListType.FAMILY).partnerName("하나사진관").couponName("가족사진 할인쿠폰").description("최대 할인금액 : 100,000원").validityDays(180).build(),
                Coupon.builder().category(BucketListType.FAMILY).partnerName("법무법인 율촌").couponName("수임료 할인쿠폰").description("최대 할인금액 : 100,000원").validityDays(180).build()
        );

        couponRepository.saveAll(initialCoupons);
    }
}
