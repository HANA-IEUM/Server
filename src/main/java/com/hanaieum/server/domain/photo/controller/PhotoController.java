package com.hanaieum.server.domain.photo.controller;

import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.photo.dto.AlbumResponse;
import com.hanaieum.server.domain.photo.dto.PhotoRequest;
import com.hanaieum.server.domain.photo.dto.PhotoResponse;
import com.hanaieum.server.domain.photo.dto.PhotoUploadResponse;
import com.hanaieum.server.domain.photo.service.PhotoService;
import com.hanaieum.server.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
@Tag(name = "Album API", description = "앨범 게시글 생성, 수정, 삭제, 조회 관련 API")

public class PhotoController {

    private final PhotoService photoService;

    // 사진 업로드
    @Operation(summary = "사진 업로드", description = "사진 파일을 업로드하고 이미지가 저장된 S3의 url을 응답받습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사진 업로드 성공"),
    })
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadImageFile(
            @RequestParam("image") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        PhotoUploadResponse photoUploadResponse = photoService.uploadImageFile(file, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "사진 업로드 성공", photoUploadResponse));
    }

    // 게시물 업로드
    @Operation(summary = "게시물 생성", description = "앨범에 게시물을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게시물 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PhotoResponse>> createPhoto(
            @Valid @RequestBody PhotoRequest photoRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PhotoResponse photoResponse = photoService.createPhoto(photoRequest, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(HttpStatus.CREATED, "게시물 생성 성공", photoResponse));
    }

    @Operation(summary = "게시물 조회", description = "하나의 게시물을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시물 조회 성공"),
    })
    @GetMapping("/{photoId}")
    public ResponseEntity<ApiResponse<PhotoResponse>> getPhoto(
            @PathVariable Long photoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PhotoResponse photoResponse = photoService.getPhoto(photoId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, photoResponse));
    }

    @Operation(summary = "앨범 전체 조회", description = "사용자가 속한 그룹 앨범의 모든 게시물을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시물 조회 성공"),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AlbumResponse>> getAlbum(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AlbumResponse album = photoService.getAlbum(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, album));
    }

    @Operation(summary = "멤버별 게시물 전체 조회", description = "특정 멤버의 모든 게시물을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "멤버별 게시물 조회 성공"),
    })
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<AlbumResponse>> getAlbumByUploader(
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AlbumResponse album = photoService.getAlbumByUploader(memberId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, album));
    }

    @Operation(summary = "게시물 수정", description = "게시물을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시물 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
    })
    @PutMapping("/{photoId}")
    public ResponseEntity<ApiResponse<PhotoResponse>> updatePhoto(
            @PathVariable Long photoId,
            @Valid @RequestBody PhotoRequest photoUpdateRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PhotoResponse photoResponse = photoService.updatePhoto(photoId, photoUpdateRequest, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "게시물 수정 성공", photoResponse));
    }

    @Operation(summary = "게시물 삭제", description = "게시물을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시물 삭제 성공"),
    })
    @DeleteMapping("/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable Long photoId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        photoService.deletePhoto(photoId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK, "게시물 삭제 성공", null));
    }

}
