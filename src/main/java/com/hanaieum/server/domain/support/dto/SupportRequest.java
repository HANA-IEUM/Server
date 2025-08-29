package com.hanaieum.server.domain.support.dto;

import com.hanaieum.server.domain.support.entity.LetterColor;
import com.hanaieum.server.domain.support.entity.SupportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "후원/응원 요청")
public class SupportRequest {

    @Schema(description = "편지지 색상", example = "PINK")
    @NotNull(message = "편지지 색상을 선택해주세요")
    private LetterColor letterColor;

    @Schema(description = "응원 메시지", example = "화이팅! 꼭 이루시길 바라요!")
    @NotBlank(message = "응원 메시지를 입력해주세요")
    @Size(max = 200, message = "응원 메시지는 200자를 초과할 수 없습니다")
    private String message;

    @Schema(description = "지원 유형 (응원/후원)", example = "SPONSOR")
    @NotNull(message = "지원 유형을 선택해주세요")
    private SupportType supportType;

    @Schema(description = "후원 금액 (응원시 null 가능)", example = "50000")
    private BigDecimal supportAmount;

    @Schema(description = "후원자의 주계좌 비밀번호 (후원시 필수)", example = "1234")
    private String accountPassword;
}