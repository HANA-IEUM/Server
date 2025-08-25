package com.hanaieum.server.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phoneNumber;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^\\d{6}$", message = "비밀번호는 6자리 숫자여야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다")
    private String name;

    @NotNull(message = "생년월일은 필수입니다")
    private LocalDate birthDate;

    @NotBlank(message = "성별은 필수입니다")
    @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F여야 합니다")
    private String gender;
}