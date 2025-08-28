package com.hanaieum.server.domain.verification.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.verification.dto.VerificationConfirmRequest;
import com.hanaieum.server.domain.verification.dto.VerificationSendRequest;
import com.hanaieum.server.domain.verification.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verification")
@Tag(name = "SMS verification API", description = "문자인증 관련 API")
@RequiredArgsConstructor
public class VerificationController {
    private final VerificationService verificationService;


    @Operation(summary = "문자 인증번호 전송", description = "입력한 전화번호로 6자리 인증번호를 발송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 전화번호 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "메시지 발송 실패")
    })
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(@Valid @RequestBody VerificationSendRequest request) {
        verificationService.sendVerificationCode(request.to());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "인증번호가 발송되었습니다.", null));
    }

    @Operation(summary = "인증번호 검증", description = "발송된 인증번호를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증번호 확인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 인증번호"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "인증번호 만료")
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmVerificationCode(@Valid @RequestBody VerificationConfirmRequest request) {
        verificationService.confirmVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "인증번호가 확인되었습니다.", null));
    }
}
