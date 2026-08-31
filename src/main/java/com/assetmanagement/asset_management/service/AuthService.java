package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.LoginRequest;
import com.assetmanagement.asset_management.dto.UserRequest;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetailsService;
import com.assetmanagement.asset_management.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DepartmentRepository departmentRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public User register(UserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException(
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException(
                    "Email already exists"
            );
        }

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Department not found"
                ));

        User user = new User();

        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setDepartment(department);

        return userRepository.save(user);
    }

    public String login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.getUsername()
                );

        return jwtService.generateToken(userDetails);
    }
}