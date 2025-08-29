package com.hanaieum.server.domain.account.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_balance_snapshots")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountBalanceSnapshot extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long id; // PK
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; // 계좌
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance; // 해당 시점 잔액
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate; // 스냅샷 일자(매일 자정)
}