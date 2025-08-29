package com.hanaieum.server.domain.moneyBox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "머니박스 채우기 요청")
public class MoneyBoxFillRequest {
    
    @NotNull(message = "이체금액은 필수입니다")
    @Positive(message = "이체금액은 양수여야 합니다")
    @Schema(description = "이체금액", example = "50000")
    private BigDecimal amount;
    
    @NotBlank(message = "계좌 비밀번호는 필수입니다")
    @Schema(description = "주계좌 비밀번호", example = "1234")
    private String accountPassword;
    
    @NotNull(message = "머니박스 계좌 ID는 필수입니다")
    @Positive(message = "머니박스 계좌 ID는 양수여야 합니다")
    @Schema(description = "머니박스 계좌 ID", example = "12345")
    private Long moneyBoxAccountId;
    
}