package com.cafun.backend.module.auth.application;

public interface LoginUseCase {
    String login(String email, String password);
}
