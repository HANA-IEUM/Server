package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListUpdateRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListDetailResponse;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;

import java.util.List;

public interface BucketListService {
    BucketListResponse createBucketList(BucketListRequest requestDto);
    
    // 분류별 버킷리스트 조회 (통합)
    List<BucketListResponse> getBucketListsByCategory(String category);

    BucketListResponse updateBucketList(Long bucketListId, BucketListUpdateRequest requestDto);

    void deleteBucketList(Long bucketListId);
    
//    // 그룹원들의 공개 버킷리스트 목록 조회
//    List<BucketListResponse> getGroupMembersBucketLists();
    
    // 특정 그룹원의 특정 버킷리스트 상세 조회
    BucketListResponse getGroupMemberBucketList(Long bucketListId);
    
    // 특정 그룹원의 버킷리스트 목록 조회
    List<BucketListResponse> getSpecificGroupMemberBucketLists(Long memberId);
    
    // 본인의 버킷리스트 상세 조회
    BucketListDetailResponse getBucketListDetail(Long bucketListId);
    
    // 버킷리스트 상태 변경
    BucketListResponse updateBucketListStatus(Long bucketListId, BucketListStatus status);
}
