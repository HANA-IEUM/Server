package com.hanaieum.server.domain.moneyBox.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyBoxUpdateRequest {
    
    @NotBlank(message = "머니박스 별명은 필수입니다.")
    @Size(max = 50, message = "머니박스 별명은 50자 이하여야 합니다.")
    private String boxName; // 머니박스 별명
    
    // 자동이체 활성화/비활성화
    @NotNull(message = "자동이체 활성화 여부는 필수입니다.")
    private Boolean autoTransferEnabled; // 자동이체 활성화 여부
    
    // 자동이체 설정 (autoTransferEnabled=true일 때만 필수) false일 경우 monthlyAmount, transferDay null 허용
    @Positive(message = "월 납입금액은 0보다 커야 합니다.")
    private BigDecimal monthlyAmount; // 월 납입금액
    
    @Min(value = 1, message = "자동이체 날짜는 1일 이상이어야 합니다.")
    @Max(value = 28, message = "자동이체 날짜는 28일 이하여야 합니다.")
    private Integer transferDay; // 자동이체 날짜 (매월)
    
    // 머니박스 수정 시 필요한 계좌 비밀번호
    @NotBlank(message = "계좌 비밀번호는 필수입니다.")
    @Size(min = 4, max = 4, message = "계좌 비밀번호는 4자리여야 합니다.")
    private String accountPassword; // 머니박스 수정 시 필수
}