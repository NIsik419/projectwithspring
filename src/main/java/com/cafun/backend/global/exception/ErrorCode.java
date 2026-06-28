package com.cafun.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(400, "잘못된 입력입니다"),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다"),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다"),
    UNAUTHORIZED(401, "인증이 필요합니다"),
    FORBIDDEN(403, "권한이 없습니다"),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다"),
    CAFE_NOT_FOUND(404, "카페를 찾을 수 없습니다"),
    INTERNAL_ERROR(500, "서버 내부 오류입니다");

    private final int status;
    private final String message;
}
