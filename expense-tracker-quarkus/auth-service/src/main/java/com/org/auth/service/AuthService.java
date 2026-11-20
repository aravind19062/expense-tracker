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
 * Registration and login. Passwords are stored as BCrypt hashes; unknown user and wrong password
 * both produce the same INVALID_CREDENTIALS error (and take the same time).
 */
@ApplicationScoped
public class AuthService {

    /** Hash of a random string; compared against when the username is unknown so both paths cost one BCrypt check. */
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("timing-equalizer-" + System.nanoTime());

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String username = normalize(request.username());
        String email = normalize(request.email());
        if (userRepository.existsByUsernameOrEmail(username, email)) {
            throw new UserAlreadyExistsException("Username or email is already registered");
        }
        User user = new User();
        user.username = username;
        user.email = email;
        user.password = BcryptUtil.bcryptHash(request.password());
        user.role = Role.USER; // self-registration can never grant ADMIN
        userRepository.persist(user);
        return user;
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByUsername(normalize(request.username())).orElse(null);
        String hash = user != null ? user.password : DUMMY_HASH;
        boolean matches = BcryptUtil.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new WrongCredException("Invalid username or password");
        }
        return tokenService.issueToken(user);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
