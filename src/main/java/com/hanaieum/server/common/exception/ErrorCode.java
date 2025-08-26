package com.hanaieum.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // JWT 관련 에러
    JWT_EXPIRED("JWT_001", "토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    JWT_INVALID("JWT_002", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    JWT_MALFORMED("JWT_003", "잘못된 형식의 토큰입니다", HttpStatus.UNAUTHORIZED),
    JWT_UNSUPPORTED("JWT_004", "지원되지 않는 토큰입니다", HttpStatus.UNAUTHORIZED),
    JWT_ILLEGAL_ARGUMENT("JWT_005", "JWT 토큰이 잘못되었습니다", HttpStatus.UNAUTHORIZED),
    JWT_SECURITY_ERROR("JWT_006", "잘못된 JWT 서명입니다", HttpStatus.UNAUTHORIZED),
    JWT_WEAK_KEY("JWT_007", "JWT 키가 약합니다", HttpStatus.INTERNAL_SERVER_ERROR),
    JWT_ERROR("JWT_008", "JWT 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // 인증 관련 에러
    UNAUTHORIZED("AUTH_001", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_002", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // 멤버 관련 에러
    MEMBER_NOT_FOUND("MEMBER_001", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_EXISTS("MEMBER_002", "이미 존재하는 전화번호입니다", HttpStatus.CONFLICT),
    INVALID_PASSWORD("MEMBER_003", "비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED),

    // 토큰 관련 에러
    REFRESH_TOKEN_NOT_FOUND("TOKEN_001", "리프레시 토큰을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXPIRED("TOKEN_002", "리프레시 토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),

    // 계좌 관련 에러
    ACCOUNT_NOT_FOUND("ACCOUNT_001", "계좌를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // 입력값 검증 에러
    INVALID_INPUT_VALUE("VALIDATION_001", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),

    // 서버 에러
    INTERNAL_SERVER_ERROR("SERVER_001", "내부 서버 오류입니다", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}