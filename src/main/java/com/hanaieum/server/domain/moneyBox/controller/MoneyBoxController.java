package com.hanaieum.server.domain.moneyBox.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;
import com.hanaieum.server.domain.moneyBox.service.MoneyBoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Money box API", description = "머니박스 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/money-box")
@RequiredArgsConstructor
public class MoneyBoxController {
    
    private final MoneyBoxService moneyBoxService;
    
    @Operation(summary = "머니박스 별명 수정", description = "머니박스의 별명을 수정합니다. 연결된 계좌 이름도 함께 변경됩니다.")
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
        log.info("머니박스 별명 수정 API 호출: accountId = {}, boxName = {}", accountId, request.getBoxName());
        
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
 
}
