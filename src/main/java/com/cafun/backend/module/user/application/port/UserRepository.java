package com.cafun.backend.module.user.application.port;

import com.cafun.backend.module.user.domain.User;

import java.util.Optional;

// application → infrastructure 방향의 output port (DIP 적용)
public interface UserRepository {
    User save(User user);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
