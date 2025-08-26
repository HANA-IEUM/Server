package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;

public interface BucketListService {
    BucketListResponse createBucketList(BucketListRequest requestDto);


}
