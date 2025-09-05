package com.hanaieum.server.domain.bucketList.calculator;

import com.hanaieum.server.domain.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class InterestCalculator {

    public BigDecimal calculateInterest(List<Transaction> transactions,
                                        LocalDate targetDate,
                                        BigDecimal targetAmount,
                                        BigDecimal interestRate) {

        // 입금 누적액
        BigDecimal depositSum = BigDecimal.ZERO;
        // 입금액 * 이자산정일수 누적
        BigDecimal weightedSum = BigDecimal.ZERO;

        for (Transaction tx : transactions) {

            // 목표금액이 다 채워졌으면 중단
            if (depositSum.compareTo(targetAmount) >= 0) break ;

            // 각 트랜잭션의 입금액(목표금액 한도)
            BigDecimal depositAmount = tx.getAmount();
            if (depositSum.add(depositAmount).compareTo(targetAmount) > 0) {
                depositAmount = targetAmount.subtract(depositSum);
            }

            // 입금 누적액
            depositSum = depositSum.add(depositAmount);

            // 각 트랜잭션별 이자 산정 일수(이체 다음날 ~ 목표일)
            long interestDays = ChronoUnit.DAYS.between(tx.getCreatedAt().toLocalDate().plusDays(1), targetDate.plusDays(1));

            // 입금액 * 이자산정일수 누적
            weightedSum = weightedSum.add(depositAmount.multiply(BigDecimal.valueOf(interestDays)));
        }

        BigDecimal totalInterest = weightedSum
                .multiply(interestRate)
                .divide(BigDecimal.valueOf(365), 0, RoundingMode.DOWN);

        return totalInterest;
    }
}
