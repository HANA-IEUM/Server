package com.hanaieum.server.domain.moneyBox.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyBoxSettingsRequest {
    
    @NotNull(message = "버킷리스트 ID는 필수입니다.")
    private Long bucketListId; // 연결할 버킷리스트 ID
    
    @NotBlank(message = "머니박스 별명은 필수입니다.")
    @Size(max = 50, message = "머니박스 별명은 50자 이하여야 합니다.")
    private String boxName; // 머니박스 별명
    
    // 월 납입 금액
    @DecimalMin(value = "0.0", inclusive = false, message = "월 납입 금액은 0보다 커야 합니다.")
    @Digits(integer = 13, fraction = 2, message = "월 납입 금액은 최대 13자리까지 가능합니다.")
    private BigDecimal monthlyPaymentAmount;
    
    // 자동이체 활성화 여부
    private Boolean autoTransferEnabled;
    
    // 자동이체 날짜 (1-31일, 자동이체 활성화 시 필수)
    @Min(value = 1, message = "자동이체 날짜는 1일 이상이어야 합니다.")
    @Max(value = 31, message = "자동이체 날짜는 31일 이하여야 합니다.")
    private Integer autoTransferDay;
    
    // 출금 계좌번호
    @Size(max = 50, message = "출금 계좌번호는 50자 이하여야 합니다.")
    private String sourceAccountNumber;
}
