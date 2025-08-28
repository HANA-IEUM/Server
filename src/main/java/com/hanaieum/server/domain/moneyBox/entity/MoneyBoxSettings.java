package com.hanaieum.server.domain.moneyBox.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    
    // 월 납입 금액
    @Column(name = "monthly_payment_amount", precision = 15, scale = 2)
    private BigDecimal monthlyPaymentAmount;
    
    // 자동이체 활성화 여부
    @Column(name = "auto_transfer_enabled")
    @Builder.Default
    private Boolean autoTransferEnabled = false;
    
    // 자동이체 날짜 (1-31일)
    @Column(name = "auto_transfer_day")
    private Integer autoTransferDay;
    
    // 출금 계좌번호 (자동이체용)
    @Column(name = "source_account_number", length = 50)
    private String sourceAccountNumber;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false; // 삭제 여부
}
