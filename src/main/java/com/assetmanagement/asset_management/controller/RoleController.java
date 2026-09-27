package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.dto.RoleRequest;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.service.RoleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR', 'ADMIN')")
    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Role createRole(@Valid @RequestBody RoleRequest request) {
        return roleService.createRole(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Role updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        return roleService.updateRole(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }
}
