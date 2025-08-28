package com.hanaieum.server.domain.transaction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferenceType {
    MANUAL("수동 이체"),
    AUTO_TRANSFER("자동이체"),
    BUCKET_ACHIEVEMENT("버킷리스트 달성"),
    BUCKET_FUNDING("버킷리스트 후원"),
    MONEY_BOX_TRANSFER("머니박스 채우기");
    
    private final String description;
}
