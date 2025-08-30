package com.hanaieum.server.domain.transaction.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.transaction.dto.TransactionResponse;
import com.hanaieum.server.domain.transaction.service.TransactionService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Transaction API", description = "거래내역 API")
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "계좌별 거래내역 조회", description = "특정 계좌의 거래내역을 페이징 처리하여 조회합니다")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionsByAccountId(
            @Parameter(description = "계좌 ID", required = true)
            @PathVariable Long accountId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("거래내역 조회 요청 - 회원 ID: {}, 계좌 ID: {}, 페이지: {}, 사이즈: {}", 
                userDetails.getId(), accountId, page, size);

        // 페이징 처리
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> transactions = transactionService.getTransactionsByAccountId(userDetails.getId(), accountId, pageable);

        log.info("거래내역 조회 성공 - 계좌 ID: {}, 총 개수: {}, 현재 페이지 개수: {}", 
                accountId, transactions.getTotalElements(), transactions.getNumberOfElements());

        return ResponseEntity.ok(ApiResponse.ok(transactions));
    }
}