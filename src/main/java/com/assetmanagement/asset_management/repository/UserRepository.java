package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> id(Long id);
    Optional<User> findByUsername(String username);
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
}