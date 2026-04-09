package com.hospital.auth.infrastructure;

import com.hospital.auth.domain.User;
import com.hospital.auth.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = jpaRepository.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
