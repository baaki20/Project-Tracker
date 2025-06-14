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
            import lombok.RequiredArgsConstructor;
            import org.springframework.data.domain.Page;
            import org.springframework.data.domain.Pageable;
            import org.springframework.security.crypto.password.PasswordEncoder;
            import org.springframework.stereotype.Service;

            import java.util.Optional;

            @Service
            @Transactional
            @RequiredArgsConstructor
            public class UserService {
                private final PasswordEncoder passwordEncoder;
                private final ContractorRepository contractorRepository;
                private final ManagerRepository managerRepository;
                private final AdminRepository adminRepository;
                private final UserRepository userRepository;

                public User createUser(UserDTO userDTO) {
                    User user = new User();
                    user.setUsername(userDTO.getUsername());
                    user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                    user.setEmail(userDTO.getEmail());
                    user.setRole(userDTO.getRole());

                    User savedUser = userRepository.save(user);

                    Role role = Role.valueOf(user.getRole());
                    if (role != null) {
                        switch (role) {
                            case CONTRACTOR -> contractorRepository.save(new Contractor(savedUser));
                            case MANAGER -> managerRepository.save(new com.buildmaster.projecttracker.entity.Manager(savedUser));
                            case ADMIN -> adminRepository.save(new Admin(savedUser));
                        }
                    }

                    return savedUser;
                }

                public Page<User> findAll(Pageable pageable) {
                    return userRepository.findAll(pageable);
                }

                public Optional<User> findById(Long id) {
                    return userRepository.findById(id);
                }

                @Transactional
                public void deleteById(Long id) {
                    User user = userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    Role role = Role.valueOf(user.getRole());
                    if (role != null) {
                        switch (role) {
                            case CONTRACTOR -> contractorRepository.deleteByUserId(id);
                            case MANAGER -> managerRepository.deleteByUserId(id);
                            case ADMIN -> adminRepository.deleteByUserId(id);
                        }
                    }

                    userRepository.deleteById(id);
                }

                public Page<User> searchUsers(String email, Pageable pageable) {
                    if (email != null && !email.isEmpty()) {
                        return userRepository.findByEmailContainingIgnoreCase(email, pageable);
                    }
                    return userRepository.findAll(pageable);
                }


                public boolean existsByEmail(String email) {
                    return userRepository.existsByEmail(email);
                }

                @Transactional
                public User updateUser(Long id, UserDTO userDTO) {
                    User user = userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    user.setUsername(userDTO.getUsername());
                    if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                    }
                    user.setEmail(userDTO.getEmail());

                    return userRepository.save(user);
                }
            }