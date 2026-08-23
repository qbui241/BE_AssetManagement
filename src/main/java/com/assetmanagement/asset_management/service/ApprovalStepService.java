package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalStepRequest;
import com.assetmanagement.asset_management.dto.ApprovalStepResponse;
import com.assetmanagement.asset_management.entity.ApprovalStep;
import com.assetmanagement.asset_management.entity.ApprovalWorkflow;
import com.assetmanagement.asset_management.entity.Role;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalStepRepository;
import com.assetmanagement.asset_management.repository.ApprovalWorkflowRepository;
import com.assetmanagement.asset_management.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalStepService {

    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final RoleRepository roleRepository;

    public ApprovalStepService(
            ApprovalStepRepository approvalStepRepository,
            ApprovalWorkflowRepository approvalWorkflowRepository,
            RoleRepository roleRepository) {

        this.approvalStepRepository = approvalStepRepository;
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.roleRepository = roleRepository;
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
                .build();
    }
}