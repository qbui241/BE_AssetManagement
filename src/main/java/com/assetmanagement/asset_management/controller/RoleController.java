package com.assetmanagement.asset_management.controller;

import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")
    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleService.createRole(role);
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @PutMapping("/{id}")
    public Role updateRole(
            @PathVariable Long id,
            @RequestBody Role role) {
        return roleService.updateRole(id, role);
    }

    @PreAuthorize("hasRole('DIRECTOR')")
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }
}