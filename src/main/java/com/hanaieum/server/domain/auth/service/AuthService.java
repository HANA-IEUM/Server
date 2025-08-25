package com.hanaieum.server.domain.auth.service;

import com.hanaieum.server.domain.auth.dto.LoginRequest;
import com.hanaieum.server.domain.auth.dto.RefreshTokenRequest;
import com.hanaieum.server.domain.auth.dto.SignupRequest;
import com.hanaieum.server.domain.auth.dto.TokenResponse;

public interface AuthService {
    
    void register(SignupRequest signupRequest);
    
    TokenResponse login(LoginRequest loginRequest);
    
    void logout(Long memberId);
    
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}