package com.hanaieum.server.domain.transaction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferenceType {
    MANUAL("이체"),
    AUTO_TRANSFER("자동이체"),
    BUCKET_FUNDING("후원"),
    MONEY_BOX_DEPOSIT("머니박스 충전"),
    MONEY_BOX_WITHDRAW("머니박스 원금 인출"),
    MONEY_BOX_INTEREST("머니박스 이자");
    
    private final String description;
}
