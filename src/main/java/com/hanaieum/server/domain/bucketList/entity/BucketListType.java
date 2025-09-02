package com.hanaieum.server.domain.bucketList.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BucketListType {

    TRIP("여행"),
    HOBBY("취미"),
    HEALTH("건강"),
    FAMILY("가족");

    private final String description;
}