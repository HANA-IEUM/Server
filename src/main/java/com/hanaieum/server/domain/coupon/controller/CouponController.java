package com.hanaieum.server.domain.coupon.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.coupon.dto.CouponResponse;
import com.hanaieum.server.domain.coupon.service.CouponService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon API", description = "쿠폰 관련 API")
public class CouponController {

    private final CouponService couponService;

    // 쿠폰 수동 생성
    @Operation(summary = "쿠폰 수동 생성", description = "테스트용 쿠폰 수동 생성 api")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "쿠폰 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "미달성된 버킷리스트"),
    })
    @PostMapping("/{bucketId}")
    public ResponseEntity<ApiResponse<Void>> createMemberCoupon(
            @PathVariable Long bucketId
    ) {
        couponService.createMemberCoupon(bucketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null));
    }

    // 쿠폰함의 모든 쿠폰 조회
    @Operation(summary = "쿠폰함의 모든 쿠폰 조회", description = "쿠폰함에 있는 모든 쿠폰들의 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "쿠폰함 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음"),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getCoupons(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        List<CouponResponse> couponResponseList = couponService.getCoupons(customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, couponResponseList));
    }
}
