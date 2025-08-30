package com.hanaieum.server.domain.account.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String number;

    @Column(name = "account_name", nullable = false, length = 50)
    private String name;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_password", nullable = false)
    private String password;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "box_name", length = 100)
    private String boxName;

    @OneToOne(mappedBy = "moneyBoxAccount", fetch = FetchType.LAZY)
    private BucketList bucketList;

    public void updateBalance(BigDecimal balance) {
        this.balance = balance;
    }

}