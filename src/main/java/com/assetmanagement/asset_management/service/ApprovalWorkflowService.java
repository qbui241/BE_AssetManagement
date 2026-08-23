package com.assetmanagement.asset_management.service;

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

    public List<ApprovalWorkflow> getAllWorkflows() {
        return approvalWorkflowRepository.findAll();
    }

    public ApprovalWorkflow getWorkflowById(Long id) {
        return approvalWorkflowRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));
    }

    public ApprovalWorkflow createWorkflow(ApprovalWorkflow workflow) {
        return approvalWorkflowRepository.save(workflow);
    }

    public ApprovalWorkflow updateWorkflow(
            Long id,
            ApprovalWorkflow payload) {

        ApprovalWorkflow workflow = getWorkflowById(id);

        workflow.setName(payload.getName());
        workflow.setDescription(payload.getDescription());
        workflow.setType(payload.getType());
        workflow.setActive(payload.isActive());

        return approvalWorkflowRepository.save(workflow);
    }

    public void deleteWorkflow(Long id) {
        ApprovalWorkflow workflow = getWorkflowById(id);
        approvalWorkflowRepository.delete(workflow);
    }
}
