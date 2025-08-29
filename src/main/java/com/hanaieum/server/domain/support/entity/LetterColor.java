package com.hanaieum.server.domain.support.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LetterColor {
    
    PINK("분홍색"),
    GREEN("초록색"),
    BLUE("파란색");
    
    private final String description;
}