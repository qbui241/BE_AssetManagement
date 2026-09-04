package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.UserRequest;
import com.assetmanagement.asset_management.dto.UserResponse;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MANAGER', 'DIRECTOR') " +
                    "or #id == authentication.principal.id"
    )
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public UserResponse createUser(@RequestBody UserRequest user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MANAGER', 'DIRECTOR') " +
                    "or #id == authentication.principal.id"
    )
    public UserResponse updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest user) {

        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<User> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        return ResponseEntity.ok(
                userService.assignRole(userId, roleId)
        );
    }
}