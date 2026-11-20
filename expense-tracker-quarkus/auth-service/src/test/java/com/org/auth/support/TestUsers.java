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
    public User find(String username) {
        return users.findByUsername(username).orElseThrow();
    }

    @Transactional
    public void setRole(String username, Role role) {
        users.findByUsername(username).orElseThrow().role = role;
    }

    @Transactional
    public long count() {
        return users.count();
    }
}
