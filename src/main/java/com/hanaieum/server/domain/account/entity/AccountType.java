package com.hanaieum.server.domain.account.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AccountType {
    MAIN("주계좌"),
    MONEY_BOX("머니박스");

    private final String description;
}
