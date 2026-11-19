package com.org.auth.support;

import com.org.auth.entity.Role;
import com.org.auth.entity.User;
import com.org.auth.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TestUsers {

    @Inject
    UserRepository users;

    @Transactional
    public void deleteAll() {
        users.deleteAll();
    }

    @Transactional
    public User find(String email) {
        return users.findByEmail(email).orElseThrow();
    }

    @Transactional
    public void setRole(String email, Role role) {
        users.findByEmail(email).orElseThrow().role = role;
    }

    @Transactional
    public long count() {
        return users.count();
    }
}
