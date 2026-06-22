package com.taskforge.service;

import com.taskforge.api.ApprovalRequest;
import com.taskforge.api.ExecutionResponse;
import com.taskforge.engine.ExecutionEngine;
import com.taskforge.exception.ExecutionNotFoundException;
import com.taskforge.exception.WorkflowNotFoundException;
import com.taskforge.handler.ApprovalTaskHandler;
import com.taskforge.model.*;
import com.taskforge.repository.TaskExecutionRepository;
import com.taskforge.repository.WorkflowDefinitionRepository;
import com.taskforge.repository.WorkflowExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final ExecutionEngine executionEngine;
    private final ApprovalTaskHandler approvalTaskHandler;

    public ExecutionService(WorkflowDefinitionRepository workflowDefinitionRepository,
                            WorkflowExecutionRepository workflowExecutionRepository,
                            TaskExecutionRepository taskExecutionRepository,
                            ExecutionEngine executionEngine,
                            ApprovalTaskHandler approvalTaskHandler) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.executionEngine = executionEngine;
        this.approvalTaskHandler = approvalTaskHandler;
    }

    // -------------------------------------------------------------------------
    // Start execution
    // -------------------------------------------------------------------------

    /**
     * Creates a WorkflowExecutionEntity (committed via its own repository transaction),
     * converts the workflow definition to a plain model, then fires the async engine.
     * Not annotated @Transactional so the save commits before the async thread starts.
     */
    public ExecutionResponse startExecution(String workflowId) {
        WorkflowDefinitionEntity wfEntity = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        // Persist execution entity — committed in its own repository transaction
        WorkflowExecutionEntity execEntity = new WorkflowExecutionEntity();
        execEntity.setId(UUID.randomUUID().toString());
        execEntity.setWorkflowDefinitionId(workflowId);
        execEntity.setStatus(ExecutionStatus.CREATED);
        WorkflowExecutionEntity saved = workflowExecutionRepository.save(execEntity);

        // Convert definition to plain model for the engine (tasks are EAGER loaded)
        WorkflowDefinition workflowDef = toModel(wfEntity);

        // Fire async execution — the entity is already committed above
        executionEngine.startExecution(saved.getId(), workflowDef);

        return toResponse(saved, Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // Get execution
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(String executionId) {
        WorkflowExecutionEntity exec = workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));
        List<TaskExecutionEntity> taskExecutions =
                taskExecutionRepository.findByWorkflowExecutionId(executionId);
        return toResponse(exec, taskExecutions);
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    public void cancelExecution(String executionId) {
        workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));
        executionEngine.requestCancellation(executionId);
    }

    // -------------------------------------------------------------------------
    // Approval
    // -------------------------------------------------------------------------

    public boolean resolveApproval(String executionId, ApprovalRequest request) {
        workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));
        return approvalTaskHandler.resolve(
                executionId, request.getTaskId(),
                request.isApproved(), request.getApprover(), request.getReason());
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private WorkflowDefinition toModel(WorkflowDefinitionEntity entity) {
        List<TaskDefinition> tasks = entity.getTasks().stream()
                .map(this::toTaskModel)
                .collect(Collectors.toList());
        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(entity.getId());
        wd.setName(entity.getName());
        wd.setTasks(tasks);
        wd.setCreatedAt(entity.getCreatedAt());
        wd.setUpdatedAt(entity.getUpdatedAt());
        return wd;
    }

    private TaskDefinition toTaskModel(TaskDefinitionEntity te) {
        TaskDefinition td = new TaskDefinition();
        td.setId(te.getTaskId());           // logical id e.g. "build"
        td.setType(te.getType());
        td.setDependsOn(te.getDependsOn() != null ? te.getDependsOn() : new ArrayList<>());
        td.setConfig(te.getConfig() != null ? te.getConfig() : new HashMap<>());
        if (te.getRetryAttempts() != null && te.getRetryAttempts() > 0) {
            RetryPolicy rp = new RetryPolicy();
            rp.setAttempts(te.getRetryAttempts());
            rp.setDelaySeconds(te.getRetryDelaySeconds() != null ? te.getRetryDelaySeconds() : 0L);
            td.setRetryPolicy(rp);
        }
        td.setTimeoutSeconds(te.getTimeoutSeconds());
        return td;
    }

    private ExecutionResponse toResponse(WorkflowExecutionEntity exec,
                                         List<TaskExecutionEntity> taskExecutions) {
        ExecutionResponse resp = new ExecutionResponse();
        resp.setId(exec.getId());
        resp.setWorkflowDefinitionId(exec.getWorkflowDefinitionId());
        resp.setStatus(exec.getStatus().name());
        resp.setStartedAt(exec.getStartedAt());
        resp.setCompletedAt(exec.getCompletedAt());

        Map<String, String> taskStatuses = new LinkedHashMap<>();
        taskExecutions.forEach(te -> taskStatuses.put(te.getTaskId(), te.getStatus().name()));
        resp.setTasks(taskStatuses);

        return resp;
    }
}

