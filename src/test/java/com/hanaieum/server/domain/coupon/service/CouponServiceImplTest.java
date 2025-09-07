package com.hanaieum.server.domain.coupon.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.coupon.dto.CouponResponse;
import com.hanaieum.server.domain.coupon.entity.Coupon;
import com.hanaieum.server.domain.coupon.entity.MemberCoupon;
import com.hanaieum.server.domain.coupon.repository.CouponRepository;
import com.hanaieum.server.domain.coupon.repository.MemberCouponRepository;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl 테스트")
class CouponServiceImplTest {

    @Mock
    private BucketListRepository bucketListRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private MemberCouponRepository memberCouponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    @DisplayName("쿠폰 코드 중복 시 새 코드를 생성한다.")
    void generateCouponCode() {
        // Given
        when(memberCouponRepository.existsByCouponCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        // When
        String couponCode = couponService.generateCouponCode();

        // Then
        assertThat(couponCode).isNotNull();
        assertThat(couponCode).hasSize(8);
        verify(memberCouponRepository, times(2)).existsByCouponCode(anyString());
    }

    @Test
    @DisplayName("목표기간별로 올바른 할인율을 반환한다.")
    void getDiscountRate() {
        // Given: 없음 (단순 값 매핑)

        // When / Then
        assertThat(couponService.getDiscountRate(3)).isEqualTo(5);
        assertThat(couponService.getDiscountRate(6)).isEqualTo(10);
        assertThat(couponService.getDiscountRate(12)).isEqualTo(15);
        assertThat(couponService.getDiscountRate(24)).isEqualTo(20);

        assertThatThrownBy(() -> couponService.getDiscountRate(9))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("완료된 버킷리스트에 대해 쿠폰을 발행할 수 있다.")
    void createMemberCoupon() {
        // Given
        Member member = buildMember();
        BucketList bucket = BucketList.builder()
                .id(1L)
                .member(member)
                .type(BucketListType.TRIP)
                .status(BucketListStatus.COMPLETED)
                .targetMonth(3)
                .build();

        Coupon coupon = buildCoupon("여행쿠폰", BucketListType.TRIP, 100);

        when(bucketListRepository.findById(1L)).thenReturn(Optional.of(bucket));
        when(couponRepository.findAllByCategory(BucketListType.TRIP)).thenReturn(List.of(coupon));
        when(memberCouponRepository.existsByCouponCode(anyString())).thenReturn(false);
        when(memberCouponRepository.save(any(MemberCoupon.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        couponService.createMemberCoupon(1L);

        // Then
        verify(memberCouponRepository, times(1)).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("버킷리스트를 찾을 수 없다면 쿠폰을 발행할 수 없다.")
    void createMemberCouponBucketNotFound() {
        // Given
        when(bucketListRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> couponService.createMemberCoupon(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.BUCKET_LIST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("완료되지 않은 버킷리스트에 대해서는 쿠폰을 발행할 수 없다.")
    void createMemberCouponInProgress() {
        // Given
        Member member = buildMember();
        BucketList bucket = BucketList.builder()
                .id(1L)
                .member(member)
                .type(BucketListType.TRIP)
                .status(BucketListStatus.IN_PROGRESS)
                .targetMonth(3)
                .build();

        when(bucketListRepository.findById(1L)).thenReturn(Optional.of(bucket));

        // When / Then
        assertThatThrownBy(() -> couponService.createMemberCoupon(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.BUCKET_LIST_NOT_COMPLETED.getMessage());
    }

    @Test
    @DisplayName("회원이 보유한 모든 쿠폰을 반환한다.")
    void getCoupons() {
        // Given
        Member member = buildMember();
        Coupon coupon = buildCoupon("여행쿠폰", BucketListType.TRIP, 100);
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .id(1L)
                .couponCode("TEST1234")
                .discountRate(10)
                .expireDate(LocalDateTime.now())
                .member(member)
                .coupon(coupon)
                .build();

        when(memberCouponRepository.findAllByMember_Id(1L))
                .thenReturn(List.of(memberCoupon));

        // When
        List<CouponResponse> responses = couponService.getCoupons(1L);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCouponCode()).isEqualTo("TEST1234");
        verify(memberCouponRepository, times(1)).findAllByMember_Id(1L);
    }

    // Helper
    private Member buildMember() {
        return Member.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .password("1234")
                .name("테스트")
                .gender(Gender.M)
                .birthDate(LocalDate.of(1990,1,1))
                .monthlyLivingCost(100000)
                .hideGroupPrompt(false)
                .build();
    }

    private Coupon buildCoupon(String name, BucketListType type, int validityDays) {
        return Coupon.builder()
                .couponName(name)
                .category(type)
                .partnerName("파트너")
                .description("설명")
                .validityDays(validityDays)
                .build();
    }

}
