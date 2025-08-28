package com.hanaieum.server.domain.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MoneyBoxCreateRequest {

    @NotBlank(message = "머니박스 이름은 필수입니다")
    @Size(max = 50, message = "머니박스 이름은 50자를 초과할 수 없습니다")
    private String accountName;

}