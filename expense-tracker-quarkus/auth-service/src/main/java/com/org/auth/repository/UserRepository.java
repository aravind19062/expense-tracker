package com.org.auth.repository;

import com.org.auth.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        return count("username = ?1 or email = ?2", username, email) > 0;
    }
}
