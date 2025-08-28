package com.hanaieum.server.domain.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class BucketAchievementRequest {

    @NotNull(message = "머니박스 계좌 ID는 필수입니다")
    private Long moneyBoxAccountId;

    @NotNull(message = "인출 금액은 필수입니다")
    @Positive(message = "인출 금액은 0보다 커야 합니다")
    private BigDecimal amount;

    @NotNull(message = "계좌 비밀번호는 필수입니다")
    @Size(min = 4, max = 6, message = "계좌 비밀번호는 4~6자리입니다")
    private String password;

    @NotNull(message = "버킷리스트 ID는 필수입니다")
    private Long bucketId;

}