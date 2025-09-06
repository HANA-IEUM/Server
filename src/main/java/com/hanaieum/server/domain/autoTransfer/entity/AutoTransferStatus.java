package com.hanaieum.server.domain.autoTransfer.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AutoTransferStatus {
    SUCCESS("성공"),
    FAILED("실패"),
    RETRY("재시도 중");
    
    private final String description;
}