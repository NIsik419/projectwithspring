package com.cafun.backend.module.user.presentation;

import com.cafun.backend.global.response.ApiResponse;
import com.cafun.backend.module.user.application.RegisterUserUseCase;
import com.cafun.backend.module.user.presentation.dto.RegisterUserRequest;
import com.cafun.backend.module.user.presentation.dto.RegisterUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        Long id = registerUserUseCase.register(request.email(), request.password(), request.nickname());
        return ApiResponse.created(new RegisterUserResponse(id, request.email(), request.nickname()));
    }
}
