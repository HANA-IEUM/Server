package com.hanaieum.server.domain.bucketList.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BucketListCategory {
    
    ALL("전체"),
    IN_PROGRESS("진행중"), 
    COMPLETED("종료"),
    PARTICIPATING("참여");
    
    private final String description;
    
    public static BucketListCategory fromString(String category) {
        if (category == null) {
            return ALL;
        }
        
        try {
            return BucketListCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
