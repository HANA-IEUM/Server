package com.hanaieum.server.domain.bucketList.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.bucketList.service.BucketListSchedulerService;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/admin/scheduler")
@Tag(name="Bucket List Scheduler API", description = "버킷리스트 스케줄러 관리 API (개발/테스트용)")
@RequiredArgsConstructor
public class BucketListSchedulerController {

    private final BucketListSchedulerService bucketListSchedulerService;
    private final BucketListRepository bucketListRepository;
    private final MemberRepository memberRepository;

    @Operation(summary = "스케줄러 상태 확인", description = "현재 만료 대상 버킷리스트 수와 상태를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스케줄러 상태 조회 성공")
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getSchedulerStatus() {
        log.info("스케줄러 상태 확인 API 호출");
        
        String status = bucketListSchedulerService.getSchedulerStatus();
        
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @Operation(summary = "스케줄러 수동 실행", description = "버킷리스트 만료 상태 업데이트 스케줄러를 수동으로 실행합니다. (테스트용)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스케줄러 수동 실행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "스케줄러 실행 중 오류 발생")
    })
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<String>> runSchedulerManually() {
        log.info("스케줄러 수동 실행 API 호출");
        
        try {
            String result = bucketListSchedulerService.runSchedulerManually();
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("스케줄러 수동 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.ok("스케줄러 실행 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "테스트용 만료 버킷리스트 생성", description = "스케줄러 테스트를 위해 만료된 버킷리스트를 생성합니다. (개발/테스트용)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 데이터 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "테스트 데이터 생성 실패")
    })
    @PostMapping("/create-test-data")
    public ResponseEntity<ApiResponse<String>> createTestExpiredBucketList(
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "1") int daysAgo) {
        
        log.info("테스트용 만료 버킷리스트 생성 API 호출: count={}, daysAgo={}", count, daysAgo);
        
        try {
            // 첫 번째 회원 조회 (테스트용)
            Member testMember = memberRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("테스트할 회원이 없습니다."));
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("테스트용 만료 버킷리스트 %d개 생성\n", count));
            result.append(String.format("목표날짜: %d일 전\n", daysAgo));
            result.append(String.format("테스트 회원: %s\n\n", testMember.getName()));
            
            LocalDate expiredDate = LocalDate.now().minusDays(daysAgo);
            
            for (int i = 1; i <= count; i++) {
                BucketList testBucketList = BucketList.builder()
                        .member(testMember)
                        .type(BucketListType.TRIP)
                        .title(String.format("테스트 만료 버킷리스트 %d", i))
                        .targetAmount(new BigDecimal("1000000"))
                        .targetDate(expiredDate)
                        .publicFlag(false)
                        .shareFlag(false)
                        .status(BucketListStatus.IN_PROGRESS)
                        .deleted(false)
                        .build();
                
                BucketList saved = bucketListRepository.save(testBucketList);
                result.append(String.format("생성된 버킷리스트: ID=%d, 제목='%s', 목표날짜=%s\n", 
                    saved.getId(), saved.getTitle(), saved.getTargetDate()));
            }
            
            return ResponseEntity.ok(ApiResponse.ok(result.toString()));
            
        } catch (Exception e) {
            log.error("테스트 데이터 생성 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.ok("테스트 데이터 생성 실패: " + e.getMessage()));
        }
    }
}
