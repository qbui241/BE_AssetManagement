package com.assetmanagement.asset_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardGroupStat {

    // Tên nhóm (vd "Laptop", "AVAILABLE", "IT (Hà Nội)", "Dell Việt Nam")
    private String label;

    // Số dòng Asset (số bản ghi, không phải số đơn vị vật lý).
    private long assetCount;

    // Tổng số ĐƠN VỊ VẬT LÝ: INDIVIDUAL luôn tính là 1, BULK tính theo
    // quantity - phản ánh đúng "tồn kho" thực tế thay vì chỉ đếm số dòng.
    private long totalUnits;

    // Số đơn vị hiện đang sẵn có (chưa ai mượn / chưa bị thanh lý).
    private long availableUnits;

    // Tổng giá trị (đơn giá x số đơn vị, cộng dồn toàn bộ nhóm).
    private BigDecimal totalValue;
}
