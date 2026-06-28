package com.cafun.backend.module.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
        @NotBlank @Size(max = 20, message = "닉네임은 20자 이하여야 합니다") String nickname
) {}
