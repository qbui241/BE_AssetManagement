package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("""
        SELECT u
        FROM User u
        LEFT JOIN FETCH u.roles
        WHERE u.username = :username
    """)
    Optional<User> findByUsernameWithRoles(
            @Param("username") String username
    );

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles
        WHERE u.id = :id
    """)
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles
    """)
    List<User> findAllWithRoles();
}