package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.UserRequest;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetHistoryRepository;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final DepartmentRepository departmentRepository;

    public UserService(
            UserRepository userRepository,
            AssetHistoryRepository assetHistoryRepository,
            DepartmentRepository departmentRepository) {

        this.userRepository = userRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found"));
    }

    public User createUser(UserRequest request) {

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("Department not found"));

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartment(department);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserRequest request) {

        User user = getUserById(id);

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("Department not found"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartment(department);

        return userRepository.save(user);
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
}
