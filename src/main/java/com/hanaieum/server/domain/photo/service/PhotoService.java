package com.hanaieum.server.domain.photo.service;

import com.hanaieum.server.domain.photo.dto.AlbumResponse;
import com.hanaieum.server.domain.photo.dto.PhotoRequest;
import com.hanaieum.server.domain.photo.dto.PhotoResponse;
import com.hanaieum.server.domain.photo.dto.PhotoUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PhotoService {
    PhotoUploadResponse uploadImageFile(MultipartFile file, Long memberId) throws IOException;
    PhotoResponse createPhoto(PhotoRequest photoRequest, Long memberId);
    PhotoResponse getPhoto(Long photoId, Long memberId);
    AlbumResponse getAlbum(Long memberId);
    AlbumResponse getAlbumByUploader(Long uploaderId, Long memberId);
    PhotoResponse updatePhoto(Long photoId, PhotoRequest photoUpdateRequest, Long memberId);
    void deletePhoto(Long photoId, Long memberId);
}
