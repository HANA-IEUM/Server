package com.hanaieum.server.domain.moneyBox.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyBoxRequest {
    
    @NotBlank(message = "머니박스 별명은 필수입니다.")
    @Size(max = 50, message = "머니박스 별명은 50자 이하여야 합니다.")
    private String boxName; // 머니박스 별명
    
    // 자동이체 수정 관련 필드 (선택사항)
    @Positive(message = "월 납입금액은 0보다 커야 합니다.")
    private BigDecimal monthlyAmount; // 월 납입금액
    
    @Min(value = 1, message = "자동이체 날짜는 1일 이상이어야 합니다.")
    @Max(value = 28, message = "자동이체 날짜는 28일 이하여야 합니다.")
    private Integer transferDay; // 자동이체 날짜 (매월)
}