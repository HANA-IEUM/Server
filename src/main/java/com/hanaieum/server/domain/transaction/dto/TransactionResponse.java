package com.hanaieum.server.domain.transaction.dto;

import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    
    private Long transactionId;
    private LocalDateTime date;
    private TransactionType transactionType;
    private String counterpartyName;
    private String description;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    
    public static TransactionResponse of(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .date(transaction.getCreatedAt())
                .transactionType(transaction.getTransactionType())
                .counterpartyName(transaction.getCounterpartyName())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .build();
    }
}