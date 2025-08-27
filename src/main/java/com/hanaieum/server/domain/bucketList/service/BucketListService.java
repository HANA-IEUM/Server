package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;

import java.util.List;

public interface BucketListService {
    BucketListResponse createBucketList(BucketListRequest requestDto);

    List<BucketListResponse> getBucketLists();

    BucketListResponse updateBucketList(Long bucketListId, BucketListRequest requestDto);

    void deleteBucketList(Long bucketListId);

    BucketListResponse getBucketListByUser(Long userId);
}
