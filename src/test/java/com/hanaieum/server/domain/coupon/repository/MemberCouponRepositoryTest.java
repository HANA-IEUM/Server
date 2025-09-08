package com.hanaieum.server.domain.coupon.repository;

import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.coupon.entity.Coupon;
import com.hanaieum.server.domain.coupon.entity.MemberCoupon;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("MemberCouponRepository 테스트")
class MemberCouponRepositoryTest {

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 ID로 회원이 보유한 모든 쿠폰을 조회한다.")
    void findAllByMember_Id() {
        // Given: Member 생성
        Member member = Member.builder()
                .phoneNumber("01012341234")
                .password("123456")
                .name("테스트멤버")
                .gender(Gender.M)
                .birthDate(LocalDate.of(1993, 1, 1))
                .monthlyLivingCost(1000000)
                .hideGroupPrompt(false)
                .build();
        member = memberRepository.save(member);

        // Given: Coupon 생성
        Coupon coupon1 = couponRepository.save(buildCoupon("여행쿠폰", BucketListType.TRIP, "하나투어", "숙박", 30));
        Coupon coupon2 = couponRepository.save(buildCoupon("건강쿠폰", BucketListType.HEALTH, "하나의료재단", "건강검진", 90));
        Coupon coupon3 = couponRepository.save(buildCoupon("취미쿠폰", BucketListType.HOBBY, "하나의료재단", "건강검진", 60));
        Coupon coupon4 = couponRepository.save(buildCoupon("가족쿠폰", BucketListType.FAMILY, "하나펫", "반려동물", 365));

        // Given: MemberCoupon 생성
        MemberCoupon memberCoupon1 = memberCouponRepository.save(buildMemberCoupon("TEST1111", 10, member, coupon1));
        MemberCoupon memberCoupon2 = memberCouponRepository.save(buildMemberCoupon("TEST2222", 10, member, coupon2));
        MemberCoupon memberCoupon3 = memberCouponRepository.save(buildMemberCoupon("TEST3333", 10, member, coupon3));
        MemberCoupon memberCoupon4 = memberCouponRepository.save(buildMemberCoupon("TEST4444", 10, member, coupon4));

        // When: MemberCoupon 조회
        List<MemberCoupon> memberCoupons = memberCouponRepository.findAllByMember_Id(member.getId());

        // Then: 개수와 주요 필드 검증
        assertThat(memberCoupons)
                .hasSize(4)
                .extracting("couponCode")
                .containsExactlyInAnyOrder("TEST1111", "TEST2222", "TEST3333", "TEST4444");
    }

    @Test
    @DisplayName("중복된 쿠폰이 있는지 확인한다.")
    void exiistsByCouponCode() {
        // Given: Member 생성
        Member member = Member.builder()
                .phoneNumber("01012341234")
                .password("123456")
                .name("테스트멤버")
                .gender(Gender.M)
                .birthDate(LocalDate.of(1993, 1, 1))
                .monthlyLivingCost(1000000)
                .hideGroupPrompt(false)
                .build();
        member = memberRepository.save(member);

        // Given: Coupon 생성
        Coupon coupon1 = couponRepository.save(buildCoupon("여행쿠폰", BucketListType.TRIP, "하나투어", "숙박", 30));
        Coupon coupon2 = couponRepository.save(buildCoupon("건강쿠폰", BucketListType.HEALTH, "하나의료재단", "건강검진", 90));
        Coupon coupon3 = couponRepository.save(buildCoupon("취미쿠폰", BucketListType.HOBBY, "하나의료재단", "건강검진", 60));
        Coupon coupon4 = couponRepository.save(buildCoupon("가족쿠폰", BucketListType.FAMILY, "하나펫", "반려동물", 365));

        // Given: MemberCoupon 생성
        MemberCoupon memberCoupon1 = memberCouponRepository.save(buildMemberCoupon("TEST1111", 10, member, coupon1));

        // When
        boolean exists = memberCouponRepository.existsByCouponCode("TEST1111");
        boolean notExists = memberCouponRepository.existsByCouponCode("TEST2222");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // Helper: Coupon 생성 메서드
    private Coupon buildCoupon(String name, BucketListType type, String partner, String description, int validityDays) {
        return Coupon.builder()
                .couponName(name)
                .category(type)
                .partnerName(partner)
                .description(description)
                .validityDays(validityDays)
                .build();
    }

    // Helper: MemberCoupon 생성 메서드
    private MemberCoupon buildMemberCoupon(String code, int discountRate, Member member, Coupon coupon) {
        return MemberCoupon.builder()
                .couponCode(code)
                .discountRate(discountRate)
                .expireDate(LocalDateTime.now().plusDays(30))
                .member(member)
                .coupon(coupon)
                .build();
    }
}
