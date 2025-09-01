package com.hanaieum.server.domain.coupon.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, name = "category")
    private BucketListType category;

    @Column(nullable = false, length = 20, name = "partner_name")
    private String partnerName;

    @Column(nullable = false, length = 10, name = "coupon_name")
    private String couponName;

    @Column(nullable = false, length = 20, name = "description")
    private String description;

    @Column(nullable = false, name = "validity_days")
    private Integer validityDays;

}
