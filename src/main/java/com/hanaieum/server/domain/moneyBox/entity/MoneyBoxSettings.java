package com.hanaieum.server.domain.moneyBox.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "money_box_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MoneyBoxSettings extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; // 머니박스 계좌 (MONEY_BOX 타입)
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", nullable = false)
    private BucketList bucketList; // 연결된 버킷리스트
    
    @Column(name = "box_name", length = 100)
    private String boxName; // 머니박스 별명
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false; // 삭제 여부
}
