package com.hanaieum.server.domain.photo.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.group.repository.GroupRepository;
import com.hanaieum.server.domain.member.dto.MemberInfoResponse;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.domain.photo.dto.AlbumResponse;
import com.hanaieum.server.domain.photo.dto.PhotoRequest;
import com.hanaieum.server.domain.photo.dto.PhotoResponse;
import com.hanaieum.server.domain.photo.dto.PhotoUploadResponse;
import com.hanaieum.server.domain.photo.entity.Photo;
import com.hanaieum.server.domain.photo.repository.PhotoRepository;
import com.hanaieum.server.infrastructure.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PhotoServiceImpl implements PhotoService {

    private final S3Service s3Service;
    private final PhotoRepository photoRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    @Override
    public PhotoUploadResponse uploadImageFile(MultipartFile file, Long memberId) throws IOException {

        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String url = s3Service.uploadFile(file);

        return PhotoUploadResponse.builder()
                .imgUrl(url)
                .build();
    }

    @Override
    public PhotoResponse createPhoto(PhotoRequest photoRequest, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Group group = member.getGroup();
        if (group == null)
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);

        Photo photo  = Photo.builder()
                .photoUrl(photoRequest.getImgUrl())
                .caption(photoRequest.getCaption())
                .uploader(member)
                .group(group)
                .build();

        photoRepository.save(photo);

        return PhotoResponse.builder()
                .photoId(photo.getId())
                .name(member.getName())
                .imgUrl(photo.getPhotoUrl())
                .caption(photo.getCaption())
                .createdAt(photo.getCreatedAt())
                .mine(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoResponse getPhoto(Long photoId, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Group group = member.getGroup();
        if (group == null)
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        boolean isUploader = photo.getUploader().getId().equals(memberId);

        return PhotoResponse.builder()
                .photoId(photo.getId())
                .name(photo.getUploader().getName())
                .imgUrl(photo.getPhotoUrl())
                .caption(photo.getCaption())
                .createdAt(photo.getCreatedAt())
                .mine(isUploader)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumResponse getAlbum(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long groupId = member.getGroup().getId();
        if (groupId == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        List<MemberInfoResponse> members = memberRepository.findAllByGroup_Id(groupId)
                .stream()
                .map(m -> new MemberInfoResponse(m.getId(), m.getName()))
                .collect(Collectors.toList());

        List<PhotoResponse> photos = photoRepository.findAllByGroup_Id(groupId)
                .stream()
                .map(photo -> PhotoResponse.builder()
                        .photoId(photo.getId())
                        .name(photo.getUploader().getName())
                        .imgUrl(photo.getPhotoUrl())
                        .caption(photo.getCaption())
                        .createdAt(photo.getCreatedAt())
                        .mine(photo.getUploader().getId().equals(memberId))
                        .build())
                .collect(Collectors.toList());

        return AlbumResponse.builder()
                .members(members)
                .photos(photos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumResponse getAlbumByUploader(Long uploaderId, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Member uploader = memberRepository.findById(uploaderId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Long groupId = member.getGroup().getId();
        if (groupId == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        List<MemberInfoResponse> members = memberRepository.findAllByGroup_Id(groupId)
                .stream()
                .map(m -> new MemberInfoResponse(m.getId(), m.getName()))
                .collect(Collectors.toList());

        List<PhotoResponse> photos = photoRepository.findAllByUploader_Id(uploaderId)
                .stream()
                .map(photo -> PhotoResponse.builder()
                        .photoId(photo.getId())
                        .name(photo.getUploader().getName())
                        .imgUrl(photo.getPhotoUrl())
                        .caption(photo.getCaption())
                        .createdAt(photo.getCreatedAt())
                        .mine(photo.getUploader().getId().equals(memberId))
                        .build())
                .collect(Collectors.toList());

        return AlbumResponse.builder()
                .members(members)
                .photos(photos)
                .build();
    }

    @Override
    public PhotoResponse updatePhoto(Long photoId,PhotoRequest photoUpdateRequest, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Group group = member.getGroup();
        if (group == null)
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        if (photo.getUploader().getId() != memberId)
            throw new CustomException(ErrorCode.PHOTO_FORBIDDEN);

        photo.setPhotoUrl(photoUpdateRequest.getImgUrl());
        photo.setCaption(photoUpdateRequest.getCaption());
        photoRepository.save(photo);

        return PhotoResponse.builder()
                .photoId(photo.getId())
                .name(member.getName())
                .imgUrl(photo.getPhotoUrl())
                .caption(photo.getCaption())
                .createdAt(photo.getCreatedAt())
                .mine(true)
                .build();
    }

    @Override
    public void deletePhoto(Long photoId, Long memberId) {

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        if (photo.getUploader().getId().equals(memberId)) {
            photoRepository.deleteById(photoId);
        }
        else throw new CustomException(ErrorCode.PHOTO_FORBIDDEN);
    }
}
