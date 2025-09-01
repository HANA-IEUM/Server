package com.hanaieum.server.domain.bucketList.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListUpdateRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListDetailResponse;
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
@Tag(name="Bucket list API", description = "버킷리스트 관련 API")
@RequiredArgsConstructor
public class BucketListController {

    private final BucketListService bucketListService;

    @Operation(summary = "버킷리스트 생성", description = "사용자가 버킷리스트를 생성합니다. createMoneyBox 옵션이 true인 경우 머니박스도 함께 생성됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "버킷리스트 생성 성공 (머니박스 포함)"),
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

    @Operation(summary = "내가 참여한 버킷리스트 목록 조회", description = "내가 구성원으로 참여한 버킷리스트 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여한 버킷리스트 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/participating")
    public ResponseEntity<ApiResponse<List<BucketListResponse>>> getParticipatingBucketLists() {
        log.info("내가 참여한 버킷리스트 목록 조회 API 호출");

        List<BucketListResponse> bucketLists = bucketListService.getParticipatingBucketLists();

        return ResponseEntity.ok(ApiResponse.ok(bucketLists));
    }

    @Operation(summary = "버킷리스트 삭제", description = "지정된 버킷리스트를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "버킷리스트 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 삭제된 버킷리스트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "버킷리스트를 찾을 수 없음")
    })
    @DeleteMapping("/{bucketListId}")
    public ResponseEntity<ApiResponse<Void>> deleteBucketList(@PathVariable Long bucketListId) {
        log.info("버킷리스트 삭제 API 호출: {}", bucketListId);
        bucketListService.deleteBucketList(bucketListId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "버킷리스트 수정", description = "버킷리스트의 제목, 공개여부, 혼자/같이 진행 여부, 그룹원 선택을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "버킷리스트 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 이미 삭제된 버킷리스트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "버킷리스트를 찾을 수 없음")
    })
    @PatchMapping("/{bucketListId}")
    public ResponseEntity<ApiResponse<BucketListResponse>> updateBucketList(
            @PathVariable Long bucketListId,
            @Valid @RequestBody BucketListUpdateRequest requestDto) {
        log.info("버킷리스트 수정 API 호출: {} - {}", bucketListId, requestDto.getTitle());
        BucketListResponse response = bucketListService.updateBucketList(bucketListId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "그룹원들의 공개 버킷리스트 목록 조회", description = "같은 그룹에 속한 멤버들의 공개 버킷리스트 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹원 버킷리스트 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/group")
    public ResponseEntity<ApiResponse<List<BucketListResponse>>> getGroupMembersBucketLists() {
        log.info("그룹원들의 공개 버킷리스트 목록 조회 API 호출");
        
        List<BucketListResponse> bucketLists = bucketListService.getGroupMembersBucketLists();
        
        return ResponseEntity.ok(ApiResponse.ok(bucketLists));
    }

    @Operation(summary = "그룹원의 특정 버킷리스트 상세 조회", description = "같은 그룹에 속한 멤버의 특정 공개 버킷리스트를 상세 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹원 버킷리스트 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "버킷리스트를 찾을 수 없음 또는 그룹이 다름")
    })
    @GetMapping("/group/{bucketListId}")
    public ResponseEntity<ApiResponse<BucketListResponse>> getGroupMemberBucketList(@PathVariable Long bucketListId) {
        log.info("그룹원의 특정 버킷리스트 상세 조회 API 호출: {}", bucketListId);
        
        BucketListResponse response = bucketListService.getGroupMemberBucketList(bucketListId);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "버킷리스트 상세 조회", description = "지정된 버킷리스트의 상세 정보를 조회합니다. 버킷리스트 이름, 목표금액, 목표기간 종료날짜 등을 포함합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "버킷리스트 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "버킷리스트를 찾을 수 없음")
    })
    @GetMapping("/{bucketListId}")
    public ResponseEntity<ApiResponse<BucketListDetailResponse>> getBucketListDetail(@PathVariable Long bucketListId) {
        log.info("버킷리스트 상세 조회 API 호출: {}", bucketListId);
        
        BucketListDetailResponse response = bucketListService.getBucketListDetail(bucketListId);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
