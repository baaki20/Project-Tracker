package com.buildmaster.projecttracker.mapper;

import com.buildmaster.projecttracker.dto.RegisterRequest;
import com.buildmaster.projecttracker.entity.User;

public class RegisterRequestMapper {
    public static User toEntity(RegisterRequest request) {
        if (request == null) return null;
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        return user;
    }
}
