package com.hanaieum.server.domain.account.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.account.dto.MainAccountResponse;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account API", description = "계좌 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "주계좌 조회", description = "사용자의 주계좌 정보를 조회합니다.")
    @GetMapping("/main")
    public ResponseEntity<ApiResponse<MainAccountResponse>> getMainAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = userDetails.getMember();
        MainAccountResponse response = accountService.getMainAccount(member);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}