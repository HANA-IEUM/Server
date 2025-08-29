package com.hanaieum.server.domain.support.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupportType {
    
    CHEER("응원"),
    SPONSOR("후원");
    
    private final String description;
}