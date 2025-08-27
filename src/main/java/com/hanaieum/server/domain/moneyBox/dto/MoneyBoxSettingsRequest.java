package com.hanaieum.server.domain.moneyBox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

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
}
