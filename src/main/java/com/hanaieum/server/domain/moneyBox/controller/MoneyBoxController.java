package com.hanaieum.server.domain.moneyBox.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxFillRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxInfoResponse;
import com.hanaieum.server.domain.moneyBox.service.MoneyBoxService;
import com.hanaieum.server.domain.transfer.service.TransferService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Money box API", description = "머니박스 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/money-box")
@RequiredArgsConstructor
public class MoneyBoxController {
    
    private final MoneyBoxService moneyBoxService;
    private final TransferService transferService;
    
    @Operation(summary = "머니박스 정보 수정", 
               description = "머니박스의 별명, 월 납입금액, 자동이체 날짜를 수정합니다. " +
                           "연결된 계좌 이름도 함께 변경되며, 자동이체 스케줄이 있는 경우 함께 업데이트됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 설정 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "머니박스를 찾을 수 없음")
    })
    @PatchMapping("/{accountId}")
    public ResponseEntity<ApiResponse<MoneyBoxResponse>> updateMoneyBoxName(
            @PathVariable Long accountId,
            @Valid @RequestBody MoneyBoxRequest request) {
        log.info("머니박스 정보 수정 API 호출: accountId = {}, boxName = {}, monthlyAmount = {}, transferDay = {}", 
                accountId, request.getBoxName(), request.getMonthlyAmount(), request.getTransferDay());
        
        MoneyBoxResponse response = moneyBoxService.updateMoneyBoxName(accountId, request);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    @Operation(summary = "내 머니박스 목록 조회", description = "현재 사용자의 모든 머니박스 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MoneyBoxResponse>>> getMyMoneyBoxList() {
        log.info("내 머니박스 목록 조회 API 호출");
        
        List<MoneyBoxResponse> response = moneyBoxService.getMyMoneyBoxList();
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    @Operation(summary = "머니박스 채우기", description = "주계좌에서 선택한 머니박스로 돈을 이체합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 채우기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "머니박스를 찾을 수 없음")
    })
    @PostMapping("/fill")
    public ResponseEntity<ApiResponse<String>> fillMoneyBox(
            @Valid @RequestBody MoneyBoxFillRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("머니박스 채우기 API 호출: 회원 ID = {}, 머니박스 ID = {}, 금액 = {}", 
                userDetails.getId(), request.getMoneyBoxAccountId(), request.getAmount());

        transferService.fillMoneyBox(userDetails.getId(), request.getMoneyBoxAccountId(),
                request.getAmount(), request.getAccountPassword());
        
        return ResponseEntity.ok(ApiResponse.ok("머니박스 채우기가 완료되었습니다."));
    }

    @Operation(summary = "머니박스 정보 조회", description = "머니박스의 상세 정보를 조회합니다 (잔액, 자동이체 설정, 연결된 버킷리스트 정보 포함).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "머니박스를 찾을 수 없음")
    })
    @GetMapping("/{boxId}/info")
    public ResponseEntity<ApiResponse<MoneyBoxInfoResponse>> getMoneyBoxInfo(@PathVariable Long boxId) {
        log.info("머니박스 정보 조회 API 호출: boxId = {}", boxId);
        
        MoneyBoxInfoResponse response = moneyBoxService.getMoneyBoxInfo(boxId);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
 
}
