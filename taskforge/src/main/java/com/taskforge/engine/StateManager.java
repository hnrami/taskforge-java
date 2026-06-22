package com.taskforge.engine;

import com.taskforge.model.*;
import com.taskforge.repository.TaskExecutionRepository;
import com.taskforge.repository.WorkflowExecutionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class StateManager {

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final TaskExecutionRepository taskExecutionRepository;

    public StateManager(WorkflowExecutionRepository workflowExecutionRepository,
                        TaskExecutionRepository taskExecutionRepository) {
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.taskExecutionRepository = taskExecutionRepository;
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /**
     * Creates a PENDING TaskExecutionEntity for every task in the workflow.
     * Called once before the execution loop starts.
     */
    @Transactional
    public void initializeTaskExecutions(String workflowExecutionId, List<TaskDefinition> tasks) {
        for (TaskDefinition task : tasks) {
            TaskExecutionEntity entity = new TaskExecutionEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWorkflowExecutionId(workflowExecutionId);
            entity.setTaskId(task.getId());
            entity.setStatus(TaskStatus.PENDING);
            entity.setAttempt(0);
            taskExecutionRepository.save(entity);
        }
    }

    // -------------------------------------------------------------------------
    // Workflow status
    // -------------------------------------------------------------------------

    @Transactional
    public void updateWorkflowStatus(String workflowExecutionId, ExecutionStatus status) {
        workflowExecutionRepository.findById(workflowExecutionId).ifPresent(we -> {
            we.setStatus(status);
            if (status == ExecutionStatus.RUNNING) {
                we.setStartedAt(Instant.now());
            }
            if (status == ExecutionStatus.SUCCESS
                    || status == ExecutionStatus.FAILED
                    || status == ExecutionStatus.CANCELLED) {
                we.setCompletedAt(Instant.now());
            }
            workflowExecutionRepository.save(we);
        });
    }

    // -------------------------------------------------------------------------
    // Task status
    // -------------------------------------------------------------------------

    @Transactional
    public void updateTaskStatus(String workflowExecutionId, String taskId, TaskStatus status) {
        findTaskExecution(workflowExecutionId, taskId).ifPresent(te -> {
            te.setStatus(status);
            if (status == TaskStatus.RUNNING) {
                te.setStartedAt(Instant.now());
            }
            if (isTerminal(status)) {
                te.setCompletedAt(Instant.now());
            }
            taskExecutionRepository.save(te);
        });
    }

    @Transactional
    public void updateTaskStatusAndAttempt(String workflowExecutionId, String taskId,
                                           TaskStatus status, int attempt, String errorMessage) {
        findTaskExecution(workflowExecutionId, taskId).ifPresent(te -> {
            te.setStatus(status);
            te.setAttempt(attempt);
            te.setErrorMessage(errorMessage);
            if (isTerminal(status)) {
                te.setCompletedAt(Instant.now());
            }
            taskExecutionRepository.save(te);
        });
    }

    // -------------------------------------------------------------------------
    // Shared outputs
    // -------------------------------------------------------------------------

    @Transactional
    public void saveTaskOutputs(String workflowExecutionId, String taskId,
                                Map<String, Object> outputs) {
        // Persist outputs on the task execution entity
        findTaskExecution(workflowExecutionId, taskId).ifPresent(te -> {
            te.setOutputs(outputs);
            taskExecutionRepository.save(te);
        });

        // Merge flattened outputs into the workflow shared context (taskId.key = value)
        workflowExecutionRepository.findById(workflowExecutionId).ifPresent(we -> {
            Map<String, Object> shared = new HashMap<>(we.getSharedOutputs());
            outputs.forEach((key, value) -> shared.put(taskId + "." + key, value));
            we.setSharedOutputs(shared);
            workflowExecutionRepository.save(we);
        });
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, TaskStatus> loadTaskStatuses(String workflowExecutionId) {
        Map<String, TaskStatus> statuses = new HashMap<>();
        taskExecutionRepository.findByWorkflowExecutionId(workflowExecutionId)
                .forEach(te -> statuses.put(te.getTaskId(), te.getStatus()));
        return statuses;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loadSharedOutputs(String workflowExecutionId) {
        return workflowExecutionRepository.findById(workflowExecutionId)
                .map(WorkflowExecutionEntity::getSharedOutputs)
                .orElse(new HashMap<>());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private java.util.Optional<TaskExecutionEntity> findTaskExecution(String workflowExecutionId, String taskId) {
        return taskExecutionRepository
                .findByWorkflowExecutionId(workflowExecutionId)
                .stream()
                .filter(te -> te.getTaskId().equals(taskId))
                .findFirst();
    }

    private boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.SUCCESS
                || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED
                || status == TaskStatus.SKIPPED;
    }
}
