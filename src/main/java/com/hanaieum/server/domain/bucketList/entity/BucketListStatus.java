package com.hanaieum.server.domain.bucketList.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BucketListStatus {

    IN_PROGRESS("진행중"),
    COMPLETED("달성완료");

    private final String description;
}
