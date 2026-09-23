package com.assetmanagement.asset_management.config;

import com.assetmanagement.asset_management.entity.*;
import com.assetmanagement.asset_management.enums.*;
import com.assetmanagement.asset_management.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Seed dữ liệu mẫu cho môi trường Docker / demo.
 *
 * Chỉ chạy MỘT LẦN: kiểm tra roleRepository.count() == 0 trước khi seed,
 * nên an toàn khi container restart nhiều lần (ddl-auto=update giữ nguyên
 * dữ liệu cũ, không bị seed chồng/lỗi unique constraint).
 *
 * KHÔNG seed approval_requests / approval_tasks / notifications / audit_logs
 * — các bảng này nên được tạo ra bằng cách demo trực tiếp trên hệ thống
 * (đúng yêu cầu đề bài: "Demo một quy trình đơn giản trên hệ thống"),
 * để người chấm thấy luồng thật thay vì dữ liệu giả lập sẵn.
 */
@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AssetRepository assetRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            RoleRepository roleRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            AssetCategoryRepository assetCategoryRepository,
            AttributeDefinitionRepository attributeDefinitionRepository,
            AssetRepository assetRepository,
            ApprovalWorkflowRepository approvalWorkflowRepository,
            ApprovalStepRepository approvalStepRepository,
            PasswordEncoder passwordEncoder) {

        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.attributeDefinitionRepository = attributeDefinitionRepository;
        this.assetRepository = assetRepository;
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("DataSeeder: đã có dữ liệu, bỏ qua seed.");
            return;
        }

        log.info("DataSeeder: database rỗng, bắt đầu seed dữ liệu demo...");

        // ---------- Roles ----------
        Role manager = roleRepository.save(new Role(null, "MANAGER", "Trưởng phòng"));
        Role director = roleRepository.save(new Role(null, "DIRECTOR", "Giám đốc"));
        Role admin = roleRepository.save(new Role(null, "ADMIN", "Quản trị hệ thống"));

        // ---------- Branches ----------
        Branch hanoi = branchRepository.save(branch("Ha Noi", "123 Cau Giay, Ha Noi"));
        Branch hcm = branchRepository.save(branch("Ho Chi Minh", "456 Nguyen Hue, Ho Chi Minh"));

        // ---------- Departments ----------
        Department itHn = departmentRepository.save(department("IT", hanoi));
        Department accHn = departmentRepository.save(department("Accounting", hanoi));
        Department bodHn = departmentRepository.save(department("Board of Directors", hanoi));

        Department itHcm = departmentRepository.save(department("IT", hcm));
        Department accHcm = departmentRepository.save(department("Accounting", hcm));
        Department bodHcm = departmentRepository.save(department("Board of Directors", hcm));

        // ---------- Users ----------
        // Mật khẩu demo: <role>@123 (đổi ngay nếu deploy thật ngoài phạm vi đồ án)
        userRepository.save(user("admin", "admin@example.com", "Quản trị viên", "Admin@123",
                bodHn, Set.of(admin)));

        userRepository.save(user("director.hn", "director.hn@example.com", "Giám đốc Hà Nội", "Director@123",
                bodHn, Set.of(director)));
        userRepository.save(user("director.hcm", "director.hcm@example.com", "Giám đốc HCM", "Director@123",
                bodHcm, Set.of(director)));

        userRepository.save(user("manager.it.hn", "manager.it.hn@example.com", "Trưởng phòng IT Hà Nội", "Manager@123",
                itHn, Set.of(manager)));
        userRepository.save(user("manager.acc.hn", "manager.acc.hn@example.com", "Trưởng phòng Kế toán Hà Nội", "Manager@123",
                accHn, Set.of(manager)));
        userRepository.save(user("manager.it.hcm", "manager.it.hcm@example.com", "Trưởng phòng IT HCM", "Manager@123",
                itHcm, Set.of(manager)));

        // Nhân viên tự đăng ký, chưa có role — đợi ADMIN gán (đúng thiết kế đã chốt)
        userRepository.save(user("employee1", "employee1@example.com", "Nhân viên IT Hà Nội", "Employee@123",
                itHn, Set.of()));
        userRepository.save(user("employee2", "employee2@example.com", "Nhân viên IT HCM", "Employee@123",
                itHcm, Set.of()));

        // ---------- Asset categories + thuộc tính động (EAV) ----------
        AssetCategory laptop = assetCategoryRepository.save(category("Laptop", "Các loại máy tính xách tay"));
        AssetCategory server = assetCategoryRepository.save(category("Server", "Máy chủ"));
        AssetCategory vehicle = assetCategoryRepository.save(category("Vehicle", "Phương tiện"));
        AssetCategory furniture = assetCategoryRepository.save(category("Furniture", "Bàn ghế, nội thất văn phòng"));

        attributeDefinitionRepository.save(attr(laptop, "cpu", "CPU", AttributeDataType.STRING, true));
        attributeDefinitionRepository.save(attr(laptop, "ram_gb", "RAM (GB)", AttributeDataType.NUMBER, true));

        attributeDefinitionRepository.save(attr(server, "cpu", "CPU", AttributeDataType.STRING, true));
        attributeDefinitionRepository.save(attr(server, "ram_gb", "RAM (GB)", AttributeDataType.NUMBER, true));
        attributeDefinitionRepository.save(attr(server, "storage_tb", "Dung lượng lưu trữ (TB)", AttributeDataType.NUMBER, false));

        attributeDefinitionRepository.save(attr(vehicle, "bien_so", "Biển số xe", AttributeDataType.STRING, true));
        attributeDefinitionRepository.save(attr(vehicle, "so_cho", "Số chỗ ngồi", AttributeDataType.NUMBER, false));

        attributeDefinitionRepository.save(attr(furniture, "material", "Chất liệu", AttributeDataType.STRING, false));
        attributeDefinitionRepository.save(attr(furniture, "color", "Màu sắc", AttributeDataType.STRING, false));

        // ---------- Assets demo ----------
        // INDIVIDUAL, giá trị dưới ngưỡng DIRECTOR (50 triệu) -> chỉ cần MANAGER duyệt
        assetRepository.save(asset("LAP-001", "Dell XPS 15", "XPS15-0001",
                new BigDecimal("36000000.00"), laptop, itHn, AssetTrackingType.INDIVIDUAL, null, null));

        // INDIVIDUAL, giá trị trên ngưỡng DIRECTOR -> cần thêm bước Giám đốc duyệt
        assetRepository.save(asset("SRV-001", "Dell PowerEdge R750", "SRV-DELL-0001",
                new BigDecimal("120000000.00"), server, itHcm, AssetTrackingType.INDIVIDUAL, null, null));

        assetRepository.save(asset("VEH-001", "Xe tải Hyundai 2.5T", "29C-88888",
                new BigDecimal("450000000.00"), vehicle, itHn, AssetTrackingType.INDIVIDUAL, null, null));

        // BULK, minh hoạ tách quantity/availableQuantity, và ngưỡng theo tổng giá trị lô
        assetRepository.save(asset("FUR-001", "Bàn làm việc gỗ", null,
                new BigDecimal("4000000.00"), furniture, itHn, AssetTrackingType.BULK, 50, 50));
        assetRepository.save(asset("FUR-002", "Ghế văn phòng", null,
                new BigDecimal("800000.00"), furniture, itHn, AssetTrackingType.BULK, 100, 100));

        // ---------- Approval workflows ----------
        ApprovalWorkflow assignmentWf = approvalWorkflowRepository.save(ApprovalWorkflow.builder()
                .name("Asset Assignment")
                .description("Quy trình phê duyệt cấp phát tài sản")
                .type(WorkflowType.SEQUENTIAL)
                .actionType(ActionType.ASSIGNMENT)
                .active(true)
                .build());

        approvalStepRepository.save(ApprovalStep.builder()
                .workflow(assignmentWf).role(manager).stepOrder(1)
                .departmentScope(DepartmentScope.REQUESTER_DEPARTMENT)
                .build());
        approvalStepRepository.save(ApprovalStep.builder()
                .workflow(assignmentWf).role(manager).stepOrder(2)
                .departmentScope(DepartmentScope.ASSET_DEPARTMENT)
                .build());
        approvalStepRepository.save(ApprovalStep.builder()
                .workflow(assignmentWf).role(director).stepOrder(3)
                .departmentScope(DepartmentScope.ASSET_BRANCH)
                .minValue(new BigDecimal("50000000.00"))
                .build());

        ApprovalWorkflow disposalWf = approvalWorkflowRepository.save(ApprovalWorkflow.builder()
                .name("Asset Disposal")
                .description("Quy trình phê duyệt thanh lý tài sản")
                .type(WorkflowType.PARALLEL)
                .actionType(ActionType.DISPOSAL)
                .active(true)
                .build());

        // Parallel: MANAGER của IT và MANAGER của Kế toán (Hà Nội) cùng duyệt thanh lý.
        approvalStepRepository.save(ApprovalStep.builder()
                .workflow(disposalWf).role(manager).stepOrder(1)
                .departmentScope(DepartmentScope.SPECIFIC_DEPARTMENT)
                .department(itHn)
                .build());
        approvalStepRepository.save(ApprovalStep.builder()
                .workflow(disposalWf).role(manager).stepOrder(1)
                .departmentScope(DepartmentScope.SPECIFIC_DEPARTMENT)
                .department(accHn)
                .build());

        log.info("DataSeeder: seed hoàn tất. Tài khoản demo: admin/Admin@123, " +
                "director.hn/Director@123, manager.it.hn/Manager@123, employee1/Employee@123 (xem thêm trong code).");
    }

    private Branch branch(String name, String address) {
        Branch b = new Branch();
        b.setName(name);
        b.setAddress(address);
        return b;
    }

    private Department department(String name, Branch branch) {
        Department d = new Department();
        d.setName(name);
        d.setBranch(branch);
        return d;
    }

    private User user(String username, String email, String name, String rawPassword,
                       Department department, Set<Role> roles) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setName(name);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setDepartment(department);
        u.setRoles(new HashSet<>(roles));
        return u;
    }

    private AssetCategory category(String name, String description) {
        AssetCategory c = new AssetCategory();
        c.setName(name);
        c.setDescription(description);
        return c;
    }

    private AttributeDefinition attr(AssetCategory category, String name, String label,
                                      AttributeDataType dataType, boolean required) {
        AttributeDefinition a = new AttributeDefinition();
        a.setCategory(category);
        a.setName(name);
        a.setLabel(label);
        a.setDataType(dataType);
        a.setRequired(required);
        return a;
    }

    private Asset asset(String assetCode, String name, String serialNumber, BigDecimal value,
                         AssetCategory category, Department department, AssetTrackingType trackingType,
                         Integer quantity, Integer availableQuantity) {
        Asset a = new Asset();
        a.setAssetCode(assetCode);
        a.setName(name);
        a.setSerialNumber(serialNumber);
        a.setValue(value);
        a.setPurchaseDate(LocalDate.now());
        a.setStatus(AssetStatus.AVAILABLE);
        a.setTrackingType(trackingType);
        a.setQuantity(quantity);
        a.setAvailableQuantity(availableQuantity);
        a.setCategory(category);
        a.setDepartment(department);
        return a;
    }
}
