package com.hanaieum.server.domain.verification.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record VerificationSendRequest(
    @NotEmpty(message = "수신번호는 필수값입니다.")
    String to
) {
}
