package com.hanaieum.server.domain.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SupportMessageUpdateRequest {
    @NotBlank(message = "메시지는 필수입니다.")
    @Size(max = 200, message = "메시지는 200자 이하여야 합니다.")
    private String message;
}