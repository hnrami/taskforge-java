package com.taskforge.service;

import com.taskforge.api.*;
import com.taskforge.engine.DagValidator;
import com.taskforge.exception.WorkflowNotFoundException;
import com.taskforge.handler.TaskHandlerRegistry;
import com.taskforge.model.*;
import com.taskforge.repository.WorkflowDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository repository;
    private final DagValidator dagValidator;
    private final TaskHandlerRegistry handlerRegistry;

    public WorkflowService(WorkflowDefinitionRepository repository,
                           DagValidator dagValidator,
                           TaskHandlerRegistry handlerRegistry) {
        this.repository = repository;
        this.dagValidator = dagValidator;
        this.handlerRegistry = handlerRegistry;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new com.taskforge.exception.WorkflowValidationException("Workflow name is required");
        }
        if (request.getTasks() == null || request.getTasks().isEmpty()) {
            throw new com.taskforge.exception.WorkflowValidationException("Workflow must have at least one task");
        }

        // Build plain task definitions for DAG validation
        List<TaskDefinition> taskDefs = request.getTasks().stream()
                .map(this::toPlainTaskDef)
                .collect(Collectors.toList());

        dagValidator.validate(taskDefs, handlerRegistry.getRegisteredTypes());

        // Build JPA entity
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setName(request.getName());

        List<TaskDefinitionEntity> taskEntities = request.getTasks().stream()
                .map(req -> buildTaskEntity(req, entity))
                .collect(Collectors.toList());

        entity.setTasks(taskEntities);

        WorkflowDefinitionEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(String id) {
        WorkflowDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException(id));
        return toResponse(entity);
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private TaskDefinition toPlainTaskDef(TaskRequest req) {
        TaskDefinition td = new TaskDefinition();
        td.setId(req.getId());
        td.setType(req.getType());
        td.setDependsOn(req.getDependsOn() != null ? req.getDependsOn() : new ArrayList<>());
        td.setConfig(req.getConfig() != null ? req.getConfig() : new HashMap<>());
        if (req.getRetryPolicy() != null) {
            RetryPolicy rp = new RetryPolicy();
            rp.setAttempts(req.getRetryPolicy().getAttempts());
            rp.setDelaySeconds(req.getRetryPolicy().getDelaySeconds());
            td.setRetryPolicy(rp);
        }
        td.setTimeoutSeconds(req.getTimeoutSeconds());
        return td;
    }

    private TaskDefinitionEntity buildTaskEntity(TaskRequest req, WorkflowDefinitionEntity parent) {
        TaskDefinitionEntity te = new TaskDefinitionEntity();
        te.setId(UUID.randomUUID().toString());
        te.setTaskId(req.getId());
        te.setType(req.getType());
        te.setDependsOn(req.getDependsOn() != null ? req.getDependsOn() : new ArrayList<>());
        te.setConfig(req.getConfig() != null ? req.getConfig() : new HashMap<>());
        if (req.getRetryPolicy() != null) {
            te.setRetryAttempts(req.getRetryPolicy().getAttempts());
            te.setRetryDelaySeconds(req.getRetryPolicy().getDelaySeconds());
        }
        te.setTimeoutSeconds(req.getTimeoutSeconds());
        te.setWorkflowDefinition(parent);
        return te;
    }

    private WorkflowResponse toResponse(WorkflowDefinitionEntity entity) {
        WorkflowResponse resp = new WorkflowResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());

        List<WorkflowResponse.TaskSummary> taskSummaries = entity.getTasks().stream()
                .map(te -> {
                    WorkflowResponse.TaskSummary ts = new WorkflowResponse.TaskSummary();
                    ts.setTaskId(te.getTaskId());
                    ts.setType(te.getType());
                    ts.setDependsOn(te.getDependsOn());
                    ts.setConfig(te.getConfig());
                    return ts;
                })
                .collect(Collectors.toList());

        resp.setTasks(taskSummaries);
        return resp;
    }
}

