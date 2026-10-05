package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.UserRequest;
import com.assetmanagement.asset_management.dto.UserResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.exception.AccessDeniedException;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetHistoryRepository;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import com.assetmanagement.asset_management.repository.RoleRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.repository.UserSpecifications;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(
            Long departmentId,
            String roleName,
            String keyword,
            Pageable pageable) {

        User currentUser = getCurrentUser();
        Long branchScope = hasRole(currentUser, ROLE_ADMIN)
                ? null
                : currentUser.getDepartment().getBranch().getId();

        var spec = UserSpecifications.withFilters(departmentId, roleName, keyword, branchScope);

        return PageResponse.from(
                userRepository.findAll(spec, pageable).map(this::toResponse)
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
        // An toan cho ca truong hop tu xem chinh minh: branch cua minh luon
        // trung branch cua minh nen validateSameBranch khong bao gio chan.
        validateSameBranch(user);
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

        validateSameBranch(department);

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

        // Kiem tra CA phong ban hien tai (nguon) LAN phong ban moi (dich), giong
        // fix da lam cho AssetService.validateDepartmentAssignment - chan truong
        // hop MANAGER "keo" mot user ngoai pham vi vao roi doi phong ban de hop
        // thuc hoa, hoac chuyen mot user trong pham vi minh sang chi nhanh khac.
        validateSameBranch(user);

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("Department not found"));

        validateSameBranch(department);

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

        validateSameBranch(user);

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

        validateSameBranch(user);

        user.getRoles().add(role);

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    // Dung chung cho getUserById / createUser / updateUser / deleteUser /
    // assignRole - moi noi dung deu can ap dung cung 1 quy tac ABAC theo branch.
    private void validateSameBranch(User targetUser) {
        User currentUser = getCurrentUser();
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        Long currentBranchId = currentUser.getDepartment().getBranch().getId();
        Long targetBranchId = targetUser.getDepartment().getBranch().getId();

        if (!currentBranchId.equals(targetBranchId)) {
            throw new AccessDeniedException(
                    "Cannot access or modify a user from a different branch."
            );
        }
    }

    // Ban cho truong hop dich la 1 Department (createUser/updateUser), khi
    // chua co doi tuong User dich de dung overload ben tren.
    private void validateSameBranch(Department targetDepartment) {
        User currentUser = getCurrentUser();
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        Long currentBranchId = currentUser.getDepartment().getBranch().getId();
        Long targetBranchId = targetDepartment.getBranch().getId();

        if (!currentBranchId.equals(targetBranchId)) {
            throw new AccessDeniedException(
                    "Cannot create or move a user into a different branch."
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