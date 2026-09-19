package com.assetmanagement.asset_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Giá trị (Value) của 1 AttributeDefinition (Attribute) trên 1 Asset (Entity)
// cụ thể -> đây là bảng "V" trong mô hình EAV.
// value luôn lưu dạng String; ép kiểu theo attributeDefinition.dataType khi
// đọc/validate ở tầng service, để không phải tạo nhiều cột value_string/
// value_number/value_date riêng biệt (đánh đổi lấy sự đơn giản, chấp nhận
// được ở quy mô đồ án).
@Entity
@Table(
        name = "asset_attribute_values",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asset_attribute_values_asset_definition",
                columnNames = {"asset_id", "attribute_definition_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    @Column(length = 500)
    private String value;
}
