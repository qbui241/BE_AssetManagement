package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.UserRequest;
import com.assetmanagement.asset_management.dto.UserResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import com.assetmanagement.asset_management.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userService.getUserById(userDetails.getUser().getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false, name = "q") String keyword,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        return userService.getAllUsers(departmentId, roleName, keyword, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN') " +
                    "or #id == authentication.principal.id"
    )
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // MANAGER/DIRECTOR chi tao duoc user trong pham vi chi nhanh minh
    // (validateSameBranch), ADMIN khong bi gioi han chi nhanh.
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public UserResponse createUser(@RequestBody UserRequest user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN') " +
                    "or #id == authentication.principal.id"
    )
    public UserResponse updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        return ResponseEntity.ok(
                userService.assignRole(userId, roleId)
        );
    }
}