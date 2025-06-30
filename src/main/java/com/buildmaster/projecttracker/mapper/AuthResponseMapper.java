package com.buildmaster.projecttracker.mapper;

import com.buildmaster.projecttracker.dto.AuthResponse;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.entity.Role;

import java.util.Set;
import java.util.stream.Collectors;

public class AuthResponseMapper {
    public static AuthResponse.UserInfo toUserInfo(User user) {
        if (user == null) return null;
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .authProvider(user.getAuthProvider())
                .roles(user.getRoles() != null ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) : null)
                .emailVerified(user.getEmailVerified())
                .build();
    }
}