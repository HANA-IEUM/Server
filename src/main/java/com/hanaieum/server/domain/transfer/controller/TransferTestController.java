package com.hanaieum.server.domain.transfer.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.transfer.dto.BucketAchievementRequest;
import com.hanaieum.server.domain.transfer.dto.MoneyBoxCreateRequest;
import com.hanaieum.server.domain.transfer.dto.SponsorRequest;
import com.hanaieum.server.domain.transfer.dto.TransferRequest;
import com.hanaieum.server.domain.transfer.service.TransferService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transfer Test API", description = "이체 테스트 API")
@RestController
@RequestMapping("/api/test/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferTestController {

    private final TransferService transferService;
    private final AccountService accountService;

    @Operation(summary = "머니박스 채우기", description = "내 주계좌에서 내 머니박스로 이체합니다")
    @PostMapping("/money-box/fill")
    public ResponseEntity<ApiResponse<String>> fillMoneyBox(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Member member = userDetails.getMember();
        
        // 사용자의 주계좌 조회
        Account mainAccount = accountService.findById(
            accountService.getMainAccount(member).getAccountId()
        );
        
        // 계좌 소유권 검증
        accountService.validateAccountOwnership(request.getToAccountId(), member.getId());
        
        log.info("머니박스 채우기 요청 - 회원 ID: {}, 출금계좌: {}, 입금계좌: {}, 금액: {}", 
                member.getId(), mainAccount.getId(), request.getToAccountId(), request.getAmount());
        
        transferService.fillMoneyBox(
            mainAccount.getId(),
            request.getToAccountId(),
            request.getAmount(),
            request.getPassword()
        );
        
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "머니박스 채우기가 완료되었습니다", null));
    }

    @Operation(summary = "머니박스 후원", description = "내 주계좌에서 다른 사람의 머니박스로 후원합니다")
    @PostMapping("/money-box/sponsor")
    public ResponseEntity<ApiResponse<String>> sponsorMoneyBox(
            @Valid @RequestBody SponsorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Member member = userDetails.getMember();
        
        // 사용자의 주계좌 조회
        Account mainAccount = accountService.findById(
            accountService.getMainAccount(member).getAccountId()
        );
        
        log.info("머니박스 후원 요청 - 후원자 ID: {}, 출금계좌: {}, 머니박스: {}, 금액: {}, 버킷 ID: {}", 
                member.getId(), mainAccount.getId(), request.getMoneyBoxAccountId(), 
                request.getAmount(), request.getBucketId());
        
        transferService.sponsorMoneyBox(
            mainAccount.getId(),
            request.getMoneyBoxAccountId(),
            request.getAmount(),
            request.getPassword(),
            request.getBucketId()
        );
        
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "머니박스 후원이 완료되었습니다", null));
    }

    @Operation(summary = "버킷리스트 달성 인출", description = "머니박스에서 주계좌로 달성 금액을 인출합니다")
    @PostMapping("/bucket/achievement")
    public ResponseEntity<ApiResponse<String>> achieveBucket(
            @Valid @RequestBody BucketAchievementRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Member member = userDetails.getMember();
        
        // 사용자의 주계좌 조회
        Account mainAccount = accountService.findById(
            accountService.getMainAccount(member).getAccountId()
        );
        
        // 머니박스 소유권 검증
        accountService.validateAccountOwnership(request.getMoneyBoxAccountId(), member.getId());
        
        log.info("버킷리스트 달성 인출 요청 - 회원 ID: {}, 머니박스: {}, 입금계좌: {}, 금액: {}, 버킷 ID: {}", 
                member.getId(), request.getMoneyBoxAccountId(), mainAccount.getId(), 
                request.getAmount(), request.getBucketId());
        
        transferService.achieveBucket(
            request.getMoneyBoxAccountId(),
            mainAccount.getId(),
            request.getAmount(),
            request.getPassword(),
            request.getBucketId()
        );
        
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "버킷리스트 달성 인출이 완료되었습니다", null));
    }

    @Operation(summary = "머니박스 계좌 생성", description = "테스트용 머니박스 계좌를 생성합니다")
    @PostMapping("/money-box/create")
    public ResponseEntity<ApiResponse<Long>> createMoneyBox(
            @Valid @RequestBody MoneyBoxCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Member member = userDetails.getMember();
        log.info("머니박스 생성 요청 - 회원 ID: {}, 계좌명: {}", member.getId(), request.getAccountName());
        
        Long accountId = accountService.createMoneyBoxAccount(member, request.getAccountName());
        
        log.info("머니박스 생성 완료 - 계좌 ID: {}", accountId);
        
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "생성 완료", accountId));
    }
}