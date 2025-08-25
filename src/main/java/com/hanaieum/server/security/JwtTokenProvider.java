package com.hanaieum.server.security;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenValidity,
            @Value("${jwt.refresh-expiration}") long refreshTokenValidity) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidity;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity;
    }

    // Access Token 발급
    public String generateAccessToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(memberId.toString()) // subject = memberId (PK)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 발급
    public String generateRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 Member ID 추출
    public Long getMemberIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (WeakKeyException e) {
            throw new CustomException(ErrorCode.JWT_WEAK_KEY, e);
        } catch (MalformedJwtException e) {
            throw new CustomException(ErrorCode.JWT_MALFORMED, e);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.JWT_EXPIRED, e);
        } catch (InvalidClaimException e) {
            throw new CustomException(ErrorCode.JWT_INVALID, e);
        } catch (SecurityException e) {
            throw new CustomException(ErrorCode.JWT_SECURITY_ERROR, e);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(ErrorCode.JWT_UNSUPPORTED, e);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.JWT_ILLEGAL_ARGUMENT, e);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.JWT_ERROR, e);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.JWT_ERROR, e);
        }
    }
}
