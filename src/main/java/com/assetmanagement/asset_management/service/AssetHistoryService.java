package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.AssetHistoryResponse;
import com.assetmanagement.asset_management.dto.PageResponse;
import com.assetmanagement.asset_management.entity.AssetHistory;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetHistoryRepository;
import com.assetmanagement.asset_management.repository.AssetHistorySpecifications;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetHistoryService {

    private final AssetHistoryRepository assetHistoryRepository;
    private final UserRepository userRepository;

    public AssetHistoryService(
            AssetHistoryRepository assetHistoryRepository,
            UserRepository userRepository) {
        this.assetHistoryRepository = assetHistoryRepository;
        this.userRepository = userRepository;
    }

    // Có phân trang + lọc. ADMIN xem toàn hệ thống, các role còn lại chỉ thấy lượt
    // mượn của tài sản thuộc chi nhánh mình (cùng nguyên tắc ABAC với GET /api/assets).
    @Transactional(readOnly = true)
    public PageResponse<AssetHistoryResponse> getAssetHistories(
            Long assetId,
            Long userId,
            boolean openOnly,
            String keyword,
            Pageable pageable) {

        User currentUser = getCurrentUser();
        Long branchScope = hasRole(currentUser, "ADMIN")
                ? null
                : currentUser.getDepartment().getBranch().getId();

        var spec = AssetHistorySpecifications.withFilters(assetId, userId, openOnly, keyword, branchScope);

        return PageResponse.from(
                assetHistoryRepository.findAll(spec, pageable).map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<AssetHistoryResponse> getMyOpenHistories() {
        User currentUser = getCurrentUser();
        var spec = AssetHistorySpecifications.withFilters(null, currentUser.getId(), true, null, null);
        return assetHistoryRepository
                .findAll(spec, org.springframework.data.domain.Sort.by("assignedAt").descending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }

    private AssetHistoryResponse toResponse(AssetHistory assetHistory) {
        AssetHistoryResponse response = new AssetHistoryResponse();

        response.setId(assetHistory.getId());
        response.setAssetId(assetHistory.getAsset().getId());
        response.setSerialNumber(assetHistory.getAsset().getSerialNumber());
        response.setCategoryName(assetHistory.getAsset().getCategory().getName());
        response.setDepartmentName(assetHistory.getAsset().getDepartment().getName());
        response.setAssetCode(assetHistory.getAsset().getAssetCode());
        response.setAssetName(assetHistory.getAsset().getName());
        response.setUserName(assetHistory.getUser().getName());
        response.setUserEmail(assetHistory.getUser().getEmail());
        response.setAssignedAt(assetHistory.getAssignedAt());
        response.setReturnedAt(assetHistory.getReturnedAt());
        response.setQuantity(assetHistory.getQuantity());

        return response;
    }
}
