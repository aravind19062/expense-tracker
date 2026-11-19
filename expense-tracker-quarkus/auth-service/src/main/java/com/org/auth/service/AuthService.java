package com.org.auth.service;

import com.org.auth.dto.LoginRequest;
import com.org.auth.dto.RegisterRequest;
import com.org.auth.entity.Role;
import com.org.auth.entity.User;
import com.org.auth.exception.UserAlreadyExistsException;
import com.org.auth.exception.WrongCredException;
import com.org.auth.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Locale;

/**
 * Migrated from the Spring {@code AuthService}. Same rules: unique email, BCrypt hashing,
 * generic "invalid credentials" on unknown email or wrong password.
 * Fix: the original threw plain RuntimeExceptions, so its 400/401 handlers never fired.
 */
@ApplicationScoped
public class AuthService {

    static final String REGISTERED = "User registered successfully";
    static final String INVALID_CREDENTIALS = "invalid credentials";
    /** Hash of a random string; compared against when the email is unknown so both paths cost one BCrypt check. */
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("timing-equalizer-" + System.nanoTime());

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }
        User user = new User();
        user.userName = request.userName().trim();
        user.email = email;
        user.password = BcryptUtil.bcryptHash(request.password());
        user.role = Role.USER; // self-registration can never grant ADMIN
        userRepository.persist(user);
        return REGISTERED;
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);
        String hash = user != null ? user.password : DUMMY_HASH;
        boolean matches = BcryptUtil.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new WrongCredException(INVALID_CREDENTIALS);
        }
        return tokenService.issueToken(user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
