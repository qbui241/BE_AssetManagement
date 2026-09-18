package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.UserRequest;
import com.assetmanagement.asset_management.dto.UserResponse;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetHistoryRepository;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import com.assetmanagement.asset_management.repository.RoleRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            AssetHistoryRepository assetHistoryRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllWithRoles()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("Department not found"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartment(department);
        user.setUsername(request.getUsername());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("Department not found"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartment(department);
        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (assetHistoryRepository.existsByUserId(id)) {
            throw new IllegalStateException(
                    "Cannot delete user because they have asset history"
            );
        }
        userRepository.delete(user);
    }

    public UserResponse assignRole(Long userId, Long roleId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role not found"));

        validateSameBranchForRoleAssignment(user);

        user.getRoles().add(role);

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    private void validateSameBranchForRoleAssignment(User targetUser) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        Long currentUserBranchId = currentUser.getDepartment().getBranch().getId();
        Long targetUserBranchId = targetUser.getDepartment().getBranch().getId();

        if (!currentUserBranchId.equals(targetUserBranchId)) {
            throw new AccessDeniedException(
                    "Cannot assign a role to a user from a different branch."
            );
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals(roleName));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .departmentId(
                        user.getDepartment() != null
                                ? user.getDepartment().getId()
                                : null
                )
                .roles(
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName())
                                .toList()
                )
                .build();
    }
}