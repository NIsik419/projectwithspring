package com.cafun.backend.module.user.application;

import com.cafun.backend.global.exception.BusinessException;
import com.cafun.backend.global.exception.ErrorCode;
import com.cafun.backend.module.user.application.port.UserRepository;
import com.cafun.backend.module.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long register(String email, String password, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.create(email, passwordEncoder.encode(password), nickname);
        return userRepository.save(user).getId();
    }
}
