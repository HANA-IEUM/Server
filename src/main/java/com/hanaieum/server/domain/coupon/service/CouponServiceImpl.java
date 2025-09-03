package com.hanaieum.server.domain.coupon.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.coupon.dto.CouponResponse;
import com.hanaieum.server.domain.coupon.entity.Coupon;
import com.hanaieum.server.domain.coupon.entity.MemberCoupon;
import com.hanaieum.server.domain.coupon.repository.CouponRepository;
import com.hanaieum.server.domain.coupon.repository.MemberCouponRepository;
import com.hanaieum.server.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final BucketListRepository bucketListRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;


    // 쿠폰코드 8자리 랜덤생성
    public String generateCouponCode() {
        final String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int length = 8;
        SecureRandom rand = new SecureRandom();

        String couponCode;
        do {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(charset.charAt(rand.nextInt(charset.length())));
            }
            couponCode = sb.toString();
        } while (memberCouponRepository.existsByCouponCode(couponCode));

        return couponCode;
    }

    public int getDiscountRate(int targetMonth) {
        return switch (targetMonth) {
            case 3 -> 5;
            case 6 -> 10;
            case 12 -> 15;
            case 24 -> 20;
            default -> throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }

    @Override
    public void createMemberCoupon(Long bucketId) {

        BucketList bucket = bucketListRepository.findById(bucketId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUCKET_LIST_NOT_FOUND));

        if (bucket.getStatus().equals(BucketListStatus.IN_PROGRESS))
            throw new CustomException(ErrorCode.BUCKET_LIST_NOT_COMPLETED);

        Member member = bucket.getMember();

        // 타입별로 쿠폰 가져오기
        List<Coupon> coupons = couponRepository.findAllByCategory(bucket.getType());
        if (coupons.isEmpty()) {
            throw new CustomException(ErrorCode.COUPON_NOT_FOUND);
        }
        // 타입별 쿠폰에서 랜덤으로 해당 타입 쿠폰 하나 가져오기
        Coupon coupon = coupons.get(new Random().nextInt(coupons.size()));

        // 버킷리스트의 targetMonth 별로 쿠폰 할인율 설정
        int discountRate = getDiscountRate(bucket.getTargetMonth());

        String couponCode = generateCouponCode();

        LocalDateTime expireDate = LocalDateTime.now()
                .plusDays(coupon.getValidityDays())
                .withHour(23).withMinute(59).withSecond(59);

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .discountRate(discountRate)
                .couponCode(couponCode)
                .expireDate(expireDate)
                .member(member)
                .coupon(coupon)
                .build();

        memberCouponRepository.save(memberCoupon);
    }

    @Override
    public List<CouponResponse> getCoupons(Long memberId) {

        List<MemberCoupon> coupons = memberCouponRepository.findAllByMember_Id(memberId);

        List<CouponResponse> couponResponses = coupons
                .stream()
                .map(coupon -> CouponResponse.builder()
                        .couponId(coupon.getId())
                        .category(coupon.getCoupon().getCategory())
                        .partnerName(coupon.getCoupon().getPartnerName())
                        .couponName(coupon.getCoupon().getCouponName())
                        .description(coupon.getCoupon().getDescription())
                        .discountRate(coupon.getDiscountRate())
                        .couponCode(coupon.getCouponCode())
                        .expireDate(coupon.getExpireDate())
                        .build())
                .collect(Collectors.toList());

        return couponResponses;
    }

}
