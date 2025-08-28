package com.hanaieum.server.domain.autoTransfer.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_transfer_schedules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutoTransferSchedule extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id; // PK
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount; // 출금 계좌
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount; // 입금 계좌
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // 이체금액
    
    @Column(name = "transfer_day", nullable = false)
    private Integer transferDay; // 이체일(매월)
    
    @Column(name = "next_transfer_date")
    private LocalDateTime nextTransferDate; // 다음 이체일
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true; // 활성화/비활성화
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false; // 삭제 여부
}