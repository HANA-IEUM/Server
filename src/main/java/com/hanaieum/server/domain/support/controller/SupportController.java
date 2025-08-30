package com.hanaieum.server.domain.support.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.support.dto.SupportRequest;
import com.hanaieum.server.domain.support.dto.SupportResponse;
import com.hanaieum.server.domain.support.service.SupportService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support API", description = "후원/응원 관리 API")
public class SupportController {

    private final SupportService supportService;

    @Operation(summary = "버킷리스트 후원/응원", description = "버킷리스트에 후원하거나 응원 메시지를 보냅니다")
    @PostMapping("/{bucketListId}")
    public ResponseEntity<ApiResponse<SupportResponse>> supportBucketList(
            @PathVariable Long bucketListId,
            @Valid @RequestBody SupportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        SupportResponse response = supportService.supportBucketList(bucketListId, request, userDetails.getMember());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "특정 버킷리스트 후원/응원 목록 조회", description = "특정 버킷리스트에 받은 후원/응원 목록을 조회합니다")
    @GetMapping("/bucket/{bucketListId}")
    public ResponseEntity<ApiResponse<List<SupportResponse>>> getBucketListSupports(
            @PathVariable Long bucketListId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        List<SupportResponse> supports = supportService.getBucketListSupports(bucketListId, userDetails.getMember());
        return ResponseEntity.ok(ApiResponse.ok(supports));
    }
}