package com.hanaieum.server.domain.coupon.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.coupon.entity.Coupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("CouponRepository 테스트")
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("카테고리별 쿠폰을 조회한다.")
    void findAllByCategory() {
        // Given: Coupon 생성 및 저장
        Coupon coupon1 = couponRepository.save(buildCoupon("여행쿠폰 1", BucketListType.TRIP, "하나투어", "숙박", 30));
        Coupon coupon2 = couponRepository.save(buildCoupon("여행쿠폰 2", BucketListType.TRIP, "대한항공", "항공", 180));
        Coupon coupon3 = couponRepository.save(buildCoupon("건강쿠폰", BucketListType.HEALTH, "하나의료재단", "건강검진", 90));
        Coupon coupon4 = couponRepository.save(buildCoupon("취미쿠폰", BucketListType.HOBBY, "하나의료재단", "건강검진", 60));
        Coupon coupon5 = couponRepository.save(buildCoupon("가족쿠폰", BucketListType.FAMILY, "하나펫", "반려동물", 365));

        // When
        List<Coupon> tripCoupons = couponRepository.findAllByCategory(BucketListType.TRIP);
        List<Coupon> healthCoupons = couponRepository.findAllByCategory(BucketListType.HEALTH);
        List<Coupon> hobbyCoupons = couponRepository.findAllByCategory(BucketListType.HOBBY);
        List<Coupon> familyCoupons = couponRepository.findAllByCategory(BucketListType.FAMILY);

        // Then
        assertThat(tripCoupons)
                .hasSize(2)
                .extracting(Coupon::getCategory)
                .containsOnly(BucketListType.TRIP);

        assertThat(healthCoupons)
                .hasSize(1)
                .extracting(Coupon::getCategory)
                .containsOnly(BucketListType.HEALTH);

        assertThat(hobbyCoupons)
                .hasSize(1)
                .extracting(Coupon::getCategory)
                .containsOnly(BucketListType.HOBBY);

        assertThat(familyCoupons)
                .hasSize(1)
                .extracting(Coupon::getCategory)
                .containsOnly(BucketListType.FAMILY);


    }

    // Helper: Coupon 생성
    private Coupon buildCoupon(String name, BucketListType category, String partner, String description, int validityDays) {
        return Coupon.builder()
                .couponName(name)
                .category(category)
                .partnerName(partner)
                .description(description)
                .validityDays(validityDays)
                .build();
    }
}
