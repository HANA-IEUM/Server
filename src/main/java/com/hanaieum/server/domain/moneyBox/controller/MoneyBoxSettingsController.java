package com.hanaieum.server.domain.moneyBox.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxSettingsRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxSettingsResponse;
import com.hanaieum.server.domain.moneyBox.service.MoneyBoxSettingsService;
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
@RequestMapping("/api/money-box-settings")
@RequiredArgsConstructor
public class MoneyBoxSettingsController {
    
    private final MoneyBoxSettingsService moneyBoxSettingsService;
    
    @Operation(
        summary = "머니박스 개별 생성 (Deprecated)", 
        description = "⚠️ 이 API는 Deprecated 되었습니다. 버킷리스트 생성 시 createMoneyBox 옵션을 사용하세요. " +
                     "이미 생성된 버킷리스트에 머니박스를 추가해야 하는 특수한 경우에만 사용하세요.",
        deprecated = true
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "머니박스 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "버킷리스트를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 해당 버킷리스트에 대한 머니박스가 존재함")
    })
    @Deprecated
    @PostMapping
    public ResponseEntity<ApiResponse<MoneyBoxSettingsResponse>> createMoneyBox(
            @Valid @RequestBody MoneyBoxSettingsRequest request) {
        log.info("머니박스 생성 API 호출: bucketListId = {}, boxName = {}", 
                request.getBucketListId(), request.getBoxName());
        
        MoneyBoxSettingsResponse response = moneyBoxSettingsService.createMoneyBox(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }
    
    @Operation(summary = "머니박스 별명 수정", description = "머니박스의 별명을 수정합니다. 연결된 계좌 이름도 함께 변경됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 설정 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "머니박스 설정을 찾을 수 없음")
    })
    @PatchMapping("/{settingsId}")
    public ResponseEntity<ApiResponse<MoneyBoxSettingsResponse>> updateMoneyBoxSettings(
            @PathVariable Long settingsId,
            @Valid @RequestBody MoneyBoxSettingsRequest request) {
        log.info("머니박스 설정 수정 API 호출: settingsId = {}, boxName = {}", settingsId, request.getBoxName());
        
        MoneyBoxSettingsResponse response = moneyBoxSettingsService.updateMoneyBoxSettings(settingsId, request);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    @Operation(summary = "머니박스 삭제", description = "머니박스를 삭제합니다. 연결된 계좌도 함께 삭제되며, 버킷리스트는 유지됩니다.", deprecated = true)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 설정 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "머니박스 설정을 찾을 수 없음")
    })
    @DeleteMapping("/{settingsId}")
    public ResponseEntity<ApiResponse<Void>> deleteMoneyBoxSettings(@PathVariable Long settingsId) {
        log.info("머니박스 설정 삭제 API 호출: settingsId = {}", settingsId);
        
        moneyBoxSettingsService.deleteMoneyBoxSettings(settingsId);
        
                return ResponseEntity.ok(ApiResponse.ok());
    }
    
    @Operation(summary = "내 머니박스 목록 조회", description = "현재 사용자의 모든 머니박스 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "머니박스 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MoneyBoxSettingsResponse>>> getMyMoneyBoxList() {
        log.info("내 머니박스 목록 조회 API 호출");
        
        List<MoneyBoxSettingsResponse> response = moneyBoxSettingsService.getMyMoneyBoxList();
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
 
}
