package com.hanaieum.server.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phoneNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 6, message = "비밀번호는 6자리여야 합니다")
    private String password;
}