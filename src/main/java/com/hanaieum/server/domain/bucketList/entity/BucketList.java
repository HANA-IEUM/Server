package com.hanaieum.server.domain.bucketList.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bucket_lists")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BucketList extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 회원

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "category")
    private BucketLIstType type; // 카테고리

    @Column(nullable = false, length = 200)
    private String title; // 제목

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount; // 목표금액

    @Column(nullable = false)
    private LocalDate targetDate; // 목표기간

    @Column(nullable = false)
    private boolean publicFlag; // 공개여부

    @Column(nullable = false)
    private boolean shareFlag; // 혼자/같이 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BucketListStatus status; // 상태

    @Column(name="is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false; // 삭제 여부
    
}
