package com.hanaieum.server.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PhoneNumberCheckRequest {
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01\\d{9}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phoneNumber;
}