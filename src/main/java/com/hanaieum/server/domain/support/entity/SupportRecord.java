package com.hanaieum.server.domain.support.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "support_records")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SupportRecord extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "support_id")
    private Long id; // PK
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", nullable = false)
    private BucketList bucketList; // 버킷리스트
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supporter_id", nullable = false)
    private Member supporter; // 응원자/후원자
    
    @Enumerated(EnumType.STRING)
    @Column(name = "support_type", nullable = false, length = 50)
    private SupportType supportType; // 유형(응원/후원)
    
    @Column(name = "support_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal supportAmount = BigDecimal.ZERO; // 후원금액 (응원시 0)
    
    @Column(length = 200)
    private String message; // 응원메시지
    
    @Enumerated(EnumType.STRING)
    @Column(name = "letter_color", length = 50)
    private LetterColor letterColor; // 편지지 색상(pink, green, blue)
}