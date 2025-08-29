package com.hanaieum.server.domain.bucketList.dto;

import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListRequest {
    
    @NotNull(message = "카테고리는 필수 입력값입니다.")
    private BucketListType type; // 카테고리
    
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title; // 제목
    
    @NotNull(message = "목표 금액은 필수 입력값입니다.")
    @Positive(message = "목표 금액은 0보다 커야 합니다.")
    private BigDecimal targetAmount; // 목표금액
    
    @NotNull(message = "목표 개월수는 필수 입력값입니다.")
    @Pattern(regexp = "^(3|6|12|24)$", message = "목표 개월수는 3, 6, 12, 24개월 중 하나여야 합니다.")
    private String targetMonths; // 목표기간(개월수)
    
    @NotNull(message = "공개 여부는 필수 입력값입니다.")
    private Boolean publicFlag; // 공개여부
    
    @NotNull(message = "혼자/함께 여부는 필수 입력값입니다.")
    private Boolean togetherFlag; // 혼자/같이 여부
    
    // togetherFlag가 true일 때만 사용
    private List<Long> selectedMemberIds; // 함께할 그룹원들의 ID 목록
    
    // 머니박스 자동 생성 관련 필드
    @Builder.Default
    private Boolean createMoneyBox = true; // 머니박스 자동 생성 여부 (기본값: true)
    
    private String moneyBoxName; // 머니박스 이름 (null이면 버킷리스트 제목 사용)
    
    // 자동이체 관련 필드
    @Builder.Default
    private Boolean enableAutoTransfer = false; // 자동이체 활성화 여부 (기본값: false)
    
    @Positive(message = "월 납입금액은 0보다 커야 합니다.")
    private BigDecimal monthlyAmount; // 월 납입금액
    
    @Pattern(regexp = "^(1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|23|24|25|26|27|28|29|30|31)$", 
             message = "이체일은 1일부터 31일 사이여야 합니다.")
    private String transferDay; // 이체일 (1-31일)
}
