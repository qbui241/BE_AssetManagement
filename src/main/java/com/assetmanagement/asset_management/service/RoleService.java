package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.RoleRequest;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.RoleRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    // 3 role lõi hệ thống - toàn bộ @PreAuthorize("hasRole('X')") trong dự
    // án (13 controller) đều phụ thuộc trực tiếp vào đúng chuỗi name này
    // tồn tại nguyên vẹn trong bảng roles. Đổi tên hoặc xoá bất kỳ role
    // nào trong danh sách này sẽ làm toàn bộ phân quyền liên quan gãy
    // ngay lập tức (không còn ai hasRole('ADMIN') được nữa chẳng hạn).
    private static final Set<String> SYSTEM_ROLE_NAMES = Set.of("MANAGER", "DIRECTOR", "ADMIN");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    @Transactional
    public Role createRole(RoleRequest request) {
        if (roleRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalStateException(
                    "Role '" + request.getName() + "' already exists"
            );
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        boolean nameChanged = !role.getName().equalsIgnoreCase(request.getName());

        // Chặn đổi TÊN của 3 role hệ thống - đây là điều kiện quan trọng
        // nhất trong toàn bộ service này. Vẫn cho phép sửa description
        // thoải mái vì không ảnh hưởng @PreAuthorize.
        if (nameChanged && SYSTEM_ROLE_NAMES.contains(role.getName())) {
            throw new IllegalStateException(
                    "Cannot rename system role '" + role.getName()
                            + "' - renaming it would break @PreAuthorize checks throughout the system"
            );
        }

        if (nameChanged && roleRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalStateException(
                    "Role '" + request.getName() + "' already exists"
            );
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // 3 role hệ thống tuyệt đối không được xoá, kể cả khi không có
        // user nào đang dùng - vì @PreAuthorize check theo tên role này
        // luôn tồn tại rải rác khắp code, xoá đi sẽ làm những chỗ đó luôn
        // luôn từ chối quyền truy cập (fail-closed, nhưng vẫn là hỏng hệ
        // thống ngoài ý muốn).
        if (SYSTEM_ROLE_NAMES.contains(role.getName())) {
            throw new IllegalStateException(
                    "Cannot delete system role '" + role.getName() + "'"
            );
        }

        long userCount = userRepository.countByRoles_Id(id);
        if (userCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete role '" + role.getName() + "': still assigned to "
                            + userCount + " user(s)"
            );
        }

        roleRepository.delete(role);
    }
}
