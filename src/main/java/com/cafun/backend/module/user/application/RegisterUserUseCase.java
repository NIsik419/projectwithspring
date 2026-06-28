package com.cafun.backend.module.user.application;

// presentation → application 방향의 input port
public interface RegisterUserUseCase {
    Long register(String email, String password, String nickname);
}
