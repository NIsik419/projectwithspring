package com.cafun.backend.module.auth.presentation;

import com.cafun.backend.global.response.ApiResponse;
import com.cafun.backend.module.auth.application.LoginUseCase;
import com.cafun.backend.module.auth.presentation.dto.LoginRequest;
import com.cafun.backend.module.auth.presentation.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ApiResponse.success(LoginResponse.of(token));
    }
}
