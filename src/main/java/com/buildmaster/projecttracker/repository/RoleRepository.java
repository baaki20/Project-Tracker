package com.buildmaster.projecttracker.repository;

import com.buildmaster.projecttracker.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("SELECT r FROM Role r WHERE r.name = :name")
    Optional<Role> findByName(@Param("name") String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNameIn(@Param("names") List<String> names);

    @Query("SELECT COUNT(u) FROM Role r JOIN r.users u WHERE r.name = :roleName")
    Long countUsersByRoleName(@Param("roleName") String roleName);
}