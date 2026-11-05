package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import com.vcraft.domain.repository.UserRepository;
import com.vcraft.infrastructure.entity.UserEntity;
import com.vcraft.infrastructure.repository.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<User> findAll(String role, int page, int limit, String search) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.searchUsers(role, search, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count(String role, String search) {
        return jpaRepository.searchUsers(role, search, PageRequest.of(0, 1)).getTotalElements();
    }

    private User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getPhone(),
                Role.valueOf(entity.getRole()),
                UserStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserEntity toEntity(User domain) {
        return new UserEntity(
                domain.id(),
                domain.email(),
                domain.passwordHash(),
                domain.fullName(),
                domain.phone(),
                domain.role().name(),
                domain.status().name(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
