package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.DashboardGroupStat;
import com.assetmanagement.asset_management.dto.DashboardStatsResponse;
import com.assetmanagement.asset_management.entity.Asset;
import com.assetmanagement.asset_management.entity.User;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.enums.AssetTrackingType;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.AssetRepository;
import com.assetmanagement.asset_management.repository.UserRepository;
import com.assetmanagement.asset_management.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class DashboardService {

    private static final String NO_SUPPLIER_LABEL = "Không rõ";

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    public DashboardService(AssetRepository assetRepository, UserRepository userRepository) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        User currentUser = getCurrentUser();

        // Cùng nguyên tắc ABAC với AssetService.getAllAssets(): ADMIN xem
        // thống kê toàn hệ thống, còn lại chỉ xem trong branch của mình.
        List<Asset> assets = hasRole(currentUser, "ADMIN")
                ? assetRepository.findAll()
                : assetRepository.findByDepartment_Branch_Id(
                currentUser.getDepartment().getBranch().getId()
        );

        long totalAssetCount = assets.size();
        long totalUnits = assets.stream().mapToLong(this::unitsOf).sum();
        long availableUnits = assets.stream().mapToLong(this::availableUnitsOf).sum();
        BigDecimal totalValue = assets.stream()
                .map(this::totalValueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardStatsResponse.builder()
                .totalAssetCount(totalAssetCount)
                .totalUnits(totalUnits)
                .availableUnits(availableUnits)
                .totalValue(totalValue)
                .byCategory(groupBy(assets, a -> a.getCategory().getName()))
                .byStatus(groupBy(assets, a -> a.getStatus().name()))
                .byDepartment(groupBy(assets, a ->
                        a.getDepartment().getName() + " (" + a.getDepartment().getBranch().getName() + ")"
                ))
                .byBranch(groupBy(assets, a -> a.getDepartment().getBranch().getName()))
                .bySupplier(groupBy(assets, a ->
                        (a.getSupplier() == null || a.getSupplier().isBlank())
                                ? NO_SUPPLIER_LABEL
                                : a.getSupplier()
                ))
                .build();
    }

    // Số đơn vị vật lý thực tế của 1 dòng asset: INDIVIDUAL luôn là 1 (1
    // dòng = 1 cái), BULK tính theo quantity (1 dòng đại diện cả lô).
    // Không dùng "đếm số dòng" đơn thuần vì sẽ sai hoàn toàn ý nghĩa "tồn
    // kho" khi có asset BULK (vd 1 dòng "100 cái ghế" không thể tính là 1).
    private long unitsOf(Asset asset) {
        if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            return 1;
        }
        return asset.getQuantity() != null ? asset.getQuantity() : 0;
    }

    private long availableUnitsOf(Asset asset) {
        if (asset.getTrackingType() == AssetTrackingType.INDIVIDUAL) {
            return asset.getStatus() == AssetStatus.AVAILABLE ? 1 : 0;
        }
        return asset.getAvailableQuantity() != null ? asset.getAvailableQuantity() : 0;
    }

    // Tổng giá trị của CẢ LÔ (đơn giá x số đơn vị) - nhất quán với cách
    // WorkflowEngineService tính ngưỡng duyệt DIRECTOR cho asset BULK.
    private BigDecimal totalValueOf(Asset asset) {
        return asset.getValue().multiply(BigDecimal.valueOf(unitsOf(asset)));
    }

    private List<DashboardGroupStat> groupBy(List<Asset> assets, Function<Asset, String> classifier) {
        Map<String, DashboardGroupStat> accumulator = new LinkedHashMap<>();

        for (Asset asset : assets) {
            String label = classifier.apply(asset);

            DashboardGroupStat current = accumulator.computeIfAbsent(label, l ->
                    DashboardGroupStat.builder()
                            .label(l)
                            .assetCount(0)
                            .totalUnits(0)
                            .availableUnits(0)
                            .totalValue(BigDecimal.ZERO)
                            .build()
            );

            current.setAssetCount(current.getAssetCount() + 1);
            current.setTotalUnits(current.getTotalUnits() + unitsOf(asset));
            current.setAvailableUnits(current.getAvailableUnits() + availableUnitsOf(asset));
            current.setTotalValue(current.getTotalValue().add(totalValueOf(asset)));
        }

        return new ArrayList<>(accumulator.values());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}
