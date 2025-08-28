package com.hanaieum.server.domain.autoTransfer.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AutoTransferStatus {
    
    ACTIVE("활성화"),
    PAUSED("일시정지"),
    STOPPED("중단");
    
    private final String description;
}