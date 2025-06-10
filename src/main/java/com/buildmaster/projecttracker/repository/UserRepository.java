package com.buildmaster.projecttracker.repository;

import com.buildmaster.projecttracker.entity.AuthProvider;
import com.buildmaster.projecttracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    List<User> findByAuthProvider(AuthProvider authProvider);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.lastLogin > :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT u FROM User u WHERE u.emailVerified = false")
    List<User> findUnverifiedUsers();

    Page<User> findByEnabledTrue(Pageable pageable);

    Page<User> findByEnabledFalse(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.authProvider = :provider")
    Long countByAuthProvider(@Param("provider") AuthProvider provider);
}