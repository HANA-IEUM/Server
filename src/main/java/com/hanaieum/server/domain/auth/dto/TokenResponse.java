package com.hanaieum.server.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private boolean hideGroupPrompt;
    private boolean mainAccountLinked;
    
    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn, boolean hideGroupPrompt, boolean mainAccountLinked) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .hideGroupPrompt(hideGroupPrompt)
                .mainAccountLinked(mainAccountLinked)
                .build();
    }
}