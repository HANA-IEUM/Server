package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListUpdateRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListDetailResponse;

import java.util.List;

public interface BucketListService {
    BucketListResponse createBucketList(BucketListRequest requestDto);

    List<BucketListResponse> getBucketLists();

    BucketListResponse updateBucketList(Long bucketListId, BucketListUpdateRequest requestDto);

    void deleteBucketList(Long bucketListId);
    
    // 그룹원들의 공개 버킷리스트 목록 조회
    List<BucketListResponse> getGroupMembersBucketLists();
    
    // 특정 그룹원의 특정 버킷리스트 상세 조회
    BucketListResponse getGroupMemberBucketList(Long bucketListId);
    
    // 본인의 버킷리스트 상세 조회
    BucketListDetailResponse getBucketListDetail(Long bucketListId);
    
    // 내가 참여한 버킷리스트 목록 조회 (구성원으로 참여한 버킷리스트)
    List<BucketListResponse> getParticipatingBucketLists();
}
