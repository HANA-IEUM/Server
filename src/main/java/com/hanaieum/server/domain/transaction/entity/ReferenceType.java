package com.hanaieum.server.domain.transaction.entity;

public enum ReferenceType {
    MANUAL,                   // 수동 이체
    AUTO_TRANSFER,            // 자동이체
    BUCKET_ACHIEVEMENT,       // 버킷리스트 달성
    BUCKET_FUNDING_SEND,      // 버킷리스트 후원 (송금)
    BUCKET_FUNDING_RECEIVE,   // 버킷리스트 후원 (수령 후 적립)
    MONEY_BOX_TRANSFER        // 머니박스 이체
}
