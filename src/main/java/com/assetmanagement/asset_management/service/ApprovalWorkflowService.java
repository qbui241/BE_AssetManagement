package com.assetmanagement.asset_management.service;

import com.assetmanagement.asset_management.dto.ApprovalWorkflowRequest;
import com.assetmanagement.asset_management.dto.ApprovalWorkflowResponse;
import com.assetmanagement.asset_management.entity.ApprovalWorkflow;
import com.assetmanagement.asset_management.exception.ResourceNotFoundException;
import com.assetmanagement.asset_management.repository.ApprovalWorkflowRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApprovalWorkflowService {

    private final ApprovalWorkflowRepository approvalWorkflowRepository;

    public ApprovalWorkflowService(
            ApprovalWorkflowRepository approvalWorkflowRepository) {
        this.approvalWorkflowRepository = approvalWorkflowRepository;
    }

    public List<ApprovalWorkflowResponse> getAllWorkflows() {
        return approvalWorkflowRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApprovalWorkflowResponse getWorkflowById(Long id) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        return toResponse(workflow);
    }

    public ApprovalWorkflowResponse createWorkflow(
            ApprovalWorkflowRequest request) {

        ApprovalWorkflow workflow = new ApprovalWorkflow();

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setType(request.getType());
        workflow.setActionType(request.getActionType());
        workflow.setActive(request.isActive());

        workflow = approvalWorkflowRepository.save(workflow);

        return toResponse(workflow);
    }

    public ApprovalWorkflowResponse updateWorkflow(
            Long id,
            ApprovalWorkflowRequest request) {

        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setType(request.getType());
        workflow.setActionType(request.getActionType());
        workflow.setActive(request.isActive());

        workflow = approvalWorkflowRepository.save(workflow);

        return toResponse(workflow);
    }

    public void deleteWorkflow(Long id) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        approvalWorkflowRepository.delete(workflow);
    }

    private ApprovalWorkflowResponse toResponse(
            ApprovalWorkflow workflow) {

        return ApprovalWorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .type(workflow.getType())
                .actionType(workflow.getActionType())
                .active(workflow.isActive())
                .build();
    }
}