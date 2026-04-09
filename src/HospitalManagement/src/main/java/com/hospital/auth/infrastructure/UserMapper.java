package com.hospital.auth.infrastructure;

import com.hospital.auth.domain.User;

class UserMapper {

    static User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .patientId(entity.getPatientId())
                .doctorId(entity.getDoctorId())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    static UserJpaEntity toEntity(User user) {
        return UserJpaEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .patientId(user.getPatientId())
                .doctorId(user.getDoctorId())
                .active(user.isActive())
                .build();
    }
}