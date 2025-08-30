package com.hanaieum.server.domain.transaction.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내 계좌
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 거래 성격 (입금/출금)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType; // DEPOSIT, WITHDRAW

    // 거래 금액
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // 거래 후 잔액 (내 계좌 기준)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    // 상대방 계좌 ID
    @Column(name = "counterparty_account_id")
    private Long counterpartyAccountId;

    // 상대방 이름 (예금주명)
    @Column(name = "counterparty_name", length = 100)
    private String counterpartyName;

    // 설명 (ex: 머니박스 채우기, 버킷 후원 등)
    @Column(length = 200)
    private String description;

    // 참조 정보 (버킷, 자동이체 등)
    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private ReferenceType referenceType;
}
