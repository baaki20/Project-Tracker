package com.buildmaster.projecttracker.service;

import com.buildmaster.projecttracker.dto.UserDTO;
import com.buildmaster.projecttracker.entity.Admin;
import com.buildmaster.projecttracker.entity.Contractor;
import com.buildmaster.projecttracker.entity.User;
import com.buildmaster.projecttracker.enums.Role;
import com.buildmaster.projecttracker.repository.AdminRepository;
import com.buildmaster.projecttracker.repository.ContractorRepository;
import com.buildmaster.projecttracker.repository.ManagerRepository;
import com.buildmaster.projecttracker.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.catalina.Manager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ContractorRepository contractorRepository;
    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private UserRepository userRepository;

    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole()); // userDTO.getRole() should return Role enum

        User savedUser = userRepository.save(user);

        // Add user to role-specific table
        if (user.getRole() == Role.CONTRACTOR) {
            contractorRepository.save(new Contractor(savedUser));
        } else if (user.getRole() == Role.MANAGER) {
            managerRepository.save(new com.buildmaster.projecttracker.entity.Manager(savedUser));
        } else if (user.getRole() == Role.ADMIN) {
            adminRepository.save(new Admin(savedUser));
        }

        return savedUser;
    }
}