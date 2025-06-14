package com.buildmaster.projecttracker.dto;

import com.buildmaster.projecttracker.entity.Role;
import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String email;
    private Role role;
}
