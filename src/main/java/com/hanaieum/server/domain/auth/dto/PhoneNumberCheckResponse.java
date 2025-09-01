package com.hanaieum.server.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneNumberCheckResponse {
    private boolean available;
}