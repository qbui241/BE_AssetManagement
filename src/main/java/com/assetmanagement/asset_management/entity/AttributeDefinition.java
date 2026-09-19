package com.assetmanagement.asset_management.entity;

import com.assetmanagement.asset_management.enums.AttributeDataType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Định nghĩa 1 thuộc tính động thuộc về 1 AssetCategory cụ thể.
// Ví dụ: category "Laptop" có các AttributeDefinition: "cpu" (STRING),
// "ram_gb" (NUMBER); category "Vehicle" có "bien_so" (STRING).
@Entity
@Table(
        name = "attribute_definitions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attribute_definitions_category_name",
                columnNames = {"category_id", "name"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    // Khoá kỹ thuật dùng để tham chiếu (vd "cpu"), không dấu, không khoảng trắng.
    @Column(nullable = false)
    private String name;

    // Tên hiển thị cho người dùng (vd "CPU", "Biển số xe").
    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeDataType dataType;

    @Column(nullable = false)
    private boolean required;
}
