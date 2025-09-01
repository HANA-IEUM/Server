package com.hanaieum.server.domain.coupon.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Long couponId;
    private BucketListType category;
    private String partnerName;
    private String couponName;
    private String description;
    private Integer discountRate;
    private String couponCode;
    private LocalDateTime expireDate;
}
