package com.hanaieum.server.domain.group.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.group.dto.GroupCreateRequest;
import com.hanaieum.server.domain.group.dto.GroupJoinRequest;
import com.hanaieum.server.domain.group.dto.GroupResponse;
import com.hanaieum.server.domain.group.service.GroupService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Group API", description = "그룹 생성, 참여, 조회 관련 API")
public class GroupController {

    private final GroupService groupService;

    // --- 그룹 생성 API ---
    @Operation(summary = "그룹 생성", description = "새로운 그룹을 생성하고 멤버를 생성된 그룹에 참여시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "그룹 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody GroupCreateRequest groupCreateRequest, // 그룹명 등을 담은 요청 DTO
            @AuthenticationPrincipal CustomUserDetails userDetails // 그룹 생성자 (인증된 사용자) 정보
    ) {
        // userDetails에서 현재 로그인된 멤버의 ID를 가져와 Service로 전달
        GroupResponse groupResponse = groupService.createGroup(groupCreateRequest, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(HttpStatus.CREATED, "그룹 생성 성공", groupResponse));
    }

    // --- 그룹 참여 API ---
    @Operation(summary = "그룹 참여", description = "초대 코드를 통해 기존 그룹에 참여합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 참여 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드")
    })
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinGroup(
            @Valid @RequestBody GroupJoinRequest groupJoinRequest, // 초대 코드 등을 담은 요청 DTO
            @AuthenticationPrincipal CustomUserDetails userDetails // 그룹에 참여하는 멤버 정보
    ) {
        groupService.joinGroup(groupJoinRequest, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "그룹 참여 성공", null)); // Void 타입으로 응답
    }

    // --- 그룹 및 그룹원 정보 조회 API ---
    @Operation(summary = "그룹 정보 조회", description = "현재 로그인된 사용자가 속한 그룹 및 그룹원 정보를 조회합니다. 그룹에 속하지 않은 경우 null을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 정보 조회 성공"),
    })
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails // 현재 로그인된 멤버 정보
    ) {
        GroupResponse groupResponse = groupService.getGroupInfo(userDetails.getId());
        if(groupResponse == null) {
            return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "NO_GROUP", null));
        }
        // 그룹에 속하지 않은 경우 null이 반환되므로, ApiResponse.ok(null)이 됨 (data 필드가 null)
        return ResponseEntity.ok(ApiResponse.ok(groupResponse));
    }

}
