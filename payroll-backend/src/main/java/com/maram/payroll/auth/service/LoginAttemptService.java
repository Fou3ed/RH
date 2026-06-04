package com.maram.payroll.auth.service;

import com.maram.payroll.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tracks failed login attempts and locks an account after {@link #MAX_ATTEMPTS}.
 *
 * <p>Uses {@code REQUIRES_NEW} so the failure counter is committed independently of
 * the (rolled-back) login transaction that threw the authentication exception.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS && !"LOCKED".equals(user.getStatus())) {
                user.setStatus("LOCKED");
                log.warn("Account '{}' locked after {} failed login attempts", username, attempts);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());
        });
    }
}
