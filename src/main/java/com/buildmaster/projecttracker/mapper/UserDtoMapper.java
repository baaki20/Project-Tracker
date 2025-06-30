package com.buildmaster.projecttracker.mapper;

import com.buildmaster.projecttracker.dto.RegisterRequest;
import com.buildmaster.projecttracker.dto.UserDTO;
import com.buildmaster.projecttracker.entity.User;

public class UserDtoMapper {
    public static UserDTO toDto(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }

    public static User toEntity(UserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        return user;
    }
}


