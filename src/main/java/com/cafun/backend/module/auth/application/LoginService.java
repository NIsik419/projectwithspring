package com.cafun.backend.module.auth.application;

import com.cafun.backend.global.exception.BusinessException;
import com.cafun.backend.global.exception.ErrorCode;
import com.cafun.backend.global.security.JwtProvider;
import com.cafun.backend.module.user.application.port.UserRepository;
import com.cafun.backend.module.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    // user.application.port.UserRepository — Port 경유이므로 모듈 간 참조 허용
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional(readOnly = true)
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return jwtProvider.generate(user.getId(), user.getEmail());
    }
}
