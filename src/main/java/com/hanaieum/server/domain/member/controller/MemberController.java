package com.hanaieum.server.domain.member.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.member.dto.MemberMypageResponse;
import com.hanaieum.server.domain.member.dto.MonthlyLivingCostResponse;
import com.hanaieum.server.domain.member.dto.MonthlyLivingCostUpdateRequest;
import com.hanaieum.server.domain.member.service.MemberService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member API", description = "회원 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "주계좌 연결", description = "사용자가 주계좌를 연결합니다.")
    @PutMapping("/main-account/link")
    public ResponseEntity<ApiResponse<Void>> confirmMainAccountLink(@AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.confirmMainAccountLink(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "주계좌가 연결되었습니다.", null));
    }

    @Operation(summary = "그룹 안내 숨김", description = "그룹 안내 문구를 더 이상 보지 않도록 설정합니다.")
    @PutMapping("/group-prompt/hide")
    public ResponseEntity<ApiResponse<Void>> hideGroupPrompt(@AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.hideGroupPrompt(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "그룹 안내가 숨겨졌습니다.", null));
    }

    @Operation(summary = "마이페이지 정보 조회", description = "사용자의 마이페이지 정보를 조회합니다.")
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MemberMypageResponse>> getMypageInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberMypageResponse response = memberService.getMypageInfo(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "월 생활비 조회", description = "사용자의 월 생활비만 조회합니다.")
    @GetMapping("/monthly-living-cost")
    public ResponseEntity<ApiResponse<MonthlyLivingCostResponse>> getMonthlyLivingCost(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MonthlyLivingCostResponse response = memberService.getMonthlyLivingCost(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "월 생활비 수정", description = "사용자의 월 생활비를 수정합니다.")
    @PutMapping("/monthly-living-cost")
    public ResponseEntity<ApiResponse<MemberMypageResponse>> updateMonthlyLivingCost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MonthlyLivingCostUpdateRequest request) {
        MemberMypageResponse response = memberService.updateMonthlyLivingCost(userDetails.getId(), request.getMonthlyLivingCost());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "월 생활비 수정이 완료되었습니다.", response));
    }
}