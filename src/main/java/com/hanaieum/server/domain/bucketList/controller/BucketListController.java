package com.hanaieum.server.domain.bucketList.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.service.BucketListService;
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

@Slf4j
@RestController
@RequestMapping("/api/bucket-lists")
@Tag(name="버킷리스트 API", description = "사용자가 버킷리스트를 생성, 수정, 삭제")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;

    @Operation(summary = "버킷리스트 생성", description = "사용자가 버킷리스트를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "버킷리스트 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BucketListResponse>> createBucketList(
            @Valid @RequestBody BucketListRequest requestDto) {
        log.info("버킷리스트 생성 API 호출");
        BucketListResponse response = bucketListService.createBucketList(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(summary = "버킷리스트 목록 조회", description = "사용자의 버킷리스트 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "버킷리스트 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<BucketListResponse>>> getBucketLists() {
        log.info("버킷리스트 목록 조회 API 호출");

        // 서비스 호출
        List<BucketListResponse> bucketLists = bucketListService.getBucketLists();

        return ResponseEntity.ok(ApiResponse.ok(bucketLists));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteBucketList() {
        return null;
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateBucketList() {
        return null;
    }

}
