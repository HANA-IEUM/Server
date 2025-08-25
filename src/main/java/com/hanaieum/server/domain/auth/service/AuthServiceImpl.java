package com.hanaieum.server.domain.auth.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.auth.dto.LoginRequest;
import com.hanaieum.server.domain.auth.dto.RefreshTokenRequest;
import com.hanaieum.server.domain.auth.dto.SignupRequest;
import com.hanaieum.server.domain.auth.dto.TokenResponse;
import com.hanaieum.server.domain.auth.entity.RefreshToken;
import com.hanaieum.server.domain.auth.repository.RefreshTokenRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    @Override
    public void register(SignupRequest signupRequest) {
        // 전화번호 중복 체크
        if (memberRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())) {
            throw new CustomException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        // Member 엔티티 생성 및 저장
        Member member = Member.builder()
                .phoneNumber(signupRequest.getPhoneNumber())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .birthDate(signupRequest.getBirthDate())
                .gender(signupRequest.getGender())
                .monthlyLivingCost(signupRequest.getMonthlyLivingCost())
                .isActive(true)
                .hideGroupPrompt(false)
                .build();

        memberRepository.save(member);
        log.info("회원가입 완료 - 전화번호: {}", signupRequest.getPhoneNumber());
    }

    @Override
    public TokenResponse login(LoginRequest loginRequest) {
        // 전화번호로 회원 조회
        Member member = memberRepository.findByPhoneNumber(loginRequest.getPhoneNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 비밀번호 확인
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 비활성화된 회원 체크
        if (!member.isActive()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(member.getId());

        // 기존 리프레시 토큰 삭제 후 새로 저장
        refreshTokenRepository.deleteByMember(member);
        refreshTokenRepository.flush(); // 삭제를 즉시 DB에 반영

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .member(member)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("로그인 성공 - 회원 ID: {}", member.getId());

        return TokenResponse.of(accessToken, refreshTokenValue, accessTokenExpiration, member.isHideGroupPrompt());
    }

    @Override
    public void logout(Long memberId) {
        // 해당 회원의 리프레시 토큰 삭제
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        refreshTokenRepository.deleteByMember(member);

        log.info("로그아웃 완료 - 회원 ID: {}", memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        // 리프레시 토큰으로 조회
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 리프레시 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken.getToken())) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(refreshToken.getMember().getId());

        log.info("토큰 갱신 완료 - 회원 ID: {}", refreshToken.getMember().getId());

        return TokenResponse.of(newAccessToken, refreshToken.getToken(), accessTokenExpiration, refreshToken.getMember().isHideGroupPrompt());
    }
}