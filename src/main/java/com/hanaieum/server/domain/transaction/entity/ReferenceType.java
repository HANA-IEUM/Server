package com.hanaieum.server.domain.transaction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferenceType {
    MANUAL("이체"),
    AUTO_TRANSFER("자동이체"),
    BUCKET_ACHIEVEMENT("목표 달성 인출"),
    BUCKET_FUNDING("후원"),
    MONEY_BOX_TRANSFER("머니박스 충전");
    
    private final String description;
}
