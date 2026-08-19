package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> id(Long id);
}