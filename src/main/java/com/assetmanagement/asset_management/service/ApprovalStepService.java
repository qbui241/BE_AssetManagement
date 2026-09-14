package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalStepRequest;
import com.assetmanagement.asset_management.dto.ApprovalStepResponse;
import com.assetmanagement.asset_management.entity.ApprovalStep;
import com.assetmanagement.asset_management.entity.ApprovalWorkflow;
import com.assetmanagement.asset_management.entity.Branch;
import com.assetmanagement.asset_management.entity.Department;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalStepRepository;
import com.assetmanagement.asset_management.repository.ApprovalWorkflowRepository;
import com.assetmanagement.asset_management.repository.BranchRepository;
import com.assetmanagement.asset_management.repository.DepartmentRepository;
import com.assetmanagement.asset_management.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalStepService {

    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;

    public ApprovalStepService(
            ApprovalStepRepository approvalStepRepository,
            ApprovalWorkflowRepository approvalWorkflowRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            BranchRepository branchRepository) {

        this.approvalStepRepository = approvalStepRepository;
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
    }

    // departmentId chi co y nghia khi departmentScope = SPECIFIC_DEPARTMENT.
    // Cac scope khac (REQUESTER_DEPARTMENT, ASSET_DEPARTMENT, *_BRANCH, ANY)
    // deu duoc resolve DONG luc tao task (xem WorkflowEngineService), khong
    // luu department tinh tren step.
    private Department resolveOptionalDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    // branchId chi co y nghia khi departmentScope = SPECIFIC_BRANCH.
    private Branch resolveOptionalBranch(Long branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private ApprovalStep findStepById(Long id) {
        return approvalStepRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Approval step not found"));
    }

    public List<ApprovalStepResponse> getAllSteps() {
        return approvalStepRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApprovalStepResponse getStepById(Long id) {
        ApprovalStep step = findStepById(id);
        return toResponse(step);
    }

    public ApprovalStepResponse createStep(ApprovalStepRequest request) {

        ApprovalWorkflow workflow = approvalWorkflowRepository
                .findById(request.getWorkflowId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        Role role = roleRepository
                .findById(request.getRoleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role not found"));

        ApprovalStep step = new ApprovalStep();

        step.setWorkflow(workflow);
        step.setRole(role);
        step.setStepOrder(request.getStepOrder());
        step.setMinValue(request.getMinValue());
        step.setMaxValue(request.getMaxValue());
        step.setDepartmentScope(request.getDepartmentScope());
        step.setDepartment(resolveOptionalDepartment(request.getDepartmentId()));
        step.setBranch(resolveOptionalBranch(request.getBranchId()));

        ApprovalStep savedStep = approvalStepRepository.save(step);

        return toResponse(savedStep);
    }

    public ApprovalStepResponse updateStep(
            Long id,
            ApprovalStepRequest request) {

        ApprovalStep step = findStepById(id);

        ApprovalWorkflow workflow = approvalWorkflowRepository
                .findById(request.getWorkflowId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        Role role = roleRepository
                .findById(request.getRoleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role not found"));

        step.setWorkflow(workflow);
        step.setRole(role);
        step.setStepOrder(request.getStepOrder());
        step.setMinValue(request.getMinValue());
        step.setMaxValue(request.getMaxValue());
        step.setDepartmentScope(request.getDepartmentScope());
        step.setDepartment(resolveOptionalDepartment(request.getDepartmentId()));
        step.setBranch(resolveOptionalBranch(request.getBranchId()));

        ApprovalStep savedStep = approvalStepRepository.save(step);

        return toResponse(savedStep);
    }

    public void deleteStep(Long id) {
        ApprovalStep step = findStepById(id);
        approvalStepRepository.delete(step);
    }

    private ApprovalStepResponse toResponse(ApprovalStep step) {

        return ApprovalStepResponse.builder()
                .id(step.getId())
                .workflowId(step.getWorkflow().getId())
                .workflowName(step.getWorkflow().getName())
                .roleId(step.getRole().getId())
                .roleName(step.getRole().getName())
                .stepOrder(step.getStepOrder())
                .minValue(step.getMinValue())
                .maxValue(step.getMaxValue())
                .departmentScope(step.getDepartmentScope())
                .departmentId(step.getDepartment() != null ? step.getDepartment().getId() : null)
                .departmentName(step.getDepartment() != null ? step.getDepartment().getName() : null)
                .branchId(step.getBranch() != null ? step.getBranch().getId() : null)
                .branchName(step.getBranch() != null ? step.getBranch().getName() : null)
                .build();
    }
}