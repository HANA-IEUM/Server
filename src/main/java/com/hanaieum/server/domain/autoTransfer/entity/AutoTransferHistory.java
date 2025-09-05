package com.hanaieum.server.domain.autoTransfer.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_transfer_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutoTransferHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private AutoTransferSchedule schedule; // 실행된 스케줄
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount; // 출금 계좌 (복원 용도)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount; // 입금 계좌 (복원 용도)

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // 이체금액 (스케줄 당시 금액 기록)

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt; // 실제 실행된 시간
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AutoTransferStatus status; // 실행 결과(성공/실패/재시도)
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason; // 실패 사유 (잔액부족 등)
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0; // 재시도 횟수
    
    // 재시도 횟수 증가
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    // 상태 변경
    public void updateStatus(AutoTransferStatus status, String failureReason) {
        this.status = status;
        this.failureReason = failureReason;
    }
}