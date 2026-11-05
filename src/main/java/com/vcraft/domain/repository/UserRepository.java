package com.vcraft.domain.repository;

import com.vcraft.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User save(User user);
    List<User> findAll(String role, int page, int limit, String search);
    long count(String role, String search);
}
