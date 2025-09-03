package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.dto.*;

import java.util.List;

public interface BucketListService {

    // 버킷리스트 생성
    BucketListResponse createBucketList(BucketListRequest requestDto);

    // 진행중인 버킷리스트 조회
    List<BucketListResponse> getInProgressBucketLists();

    // 완료된 버킷리스트 조회
    List<BucketListResponse> getCompletedBucketLists();

    // 참여중인 버킷리스트 조회
    List<BucketListResponse> getParticipatedBucketLists();

    // 본인의 버킷리스트 상세 조회
    MyBucketListDetailResponse getBucketListDetail(Long bucketListId);

    // 그룹원의 진행중인 버킷리스트 목록 조회
    List<BucketListResponse> getGroupInProgressBucketLists(Long groupMemberId);

    // 그룹원의 완료된 버킷리스트 목록 조회
    List<BucketListResponse> getGroupCompletedBucketLists(Long groupMemberId);

    // 특정 그룹원의 특정 버킷리스트 상세 조회
    GroupBucketListDetailResponse getGroupMemberBucketList(Long bucketListId);

    // 버킷리스트 수정
    BucketListResponse updateBucketList(Long bucketListId, BucketListUpdateRequest requestDto);

    // 버킷리스트 삭제
    void deleteBucketList(Long bucketListId);

    // 버킷리스트 완료 처리
    BucketListResponse completeBucketList(Long bucketListId);

    // 버킷리스트 생성 가능 여부 확인
    BucketListCreationAvailabilityResponse checkCreationAvailability();

}
