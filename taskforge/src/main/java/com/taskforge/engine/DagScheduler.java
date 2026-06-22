package com.taskforge.engine;

import com.taskforge.model.TaskDefinition;
import com.taskforge.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DagScheduler {

    /**
     * Returns all tasks that are PENDING and have every declared dependency in SUCCESS state.
     */
    public List<TaskDefinition> getReadyTasks(List<TaskDefinition> allTasks,
                                               Map<String, TaskStatus> taskStatuses) {
        return allTasks.stream()
                .filter(task -> isReady(task, taskStatuses))
                .collect(Collectors.toList());
    }

    /**
     * Returns true when no task is still PENDING, RUNNING, or WAITING_APPROVAL.
     */
    public boolean isFinished(Map<String, TaskStatus> taskStatuses) {
        return taskStatuses.values().stream()
                .noneMatch(s -> s == TaskStatus.PENDING
                        || s == TaskStatus.RUNNING
                        || s == TaskStatus.WAITING_APPROVAL);
    }

    /**
     * Returns true if any task is in FAILED state.
     */
    public boolean hasFailed(Map<String, TaskStatus> taskStatuses) {
        return taskStatuses.values().stream().anyMatch(s -> s == TaskStatus.FAILED);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isReady(TaskDefinition task, Map<String, TaskStatus> taskStatuses) {
        TaskStatus current = taskStatuses.getOrDefault(task.getId(), TaskStatus.PENDING);
        if (current != TaskStatus.PENDING) {
            return false;
        }
        List<String> deps = task.getDependsOn();
        if (deps == null || deps.isEmpty()) {
            return true;
        }
        return deps.stream()
                .allMatch(dep -> taskStatuses.getOrDefault(dep, TaskStatus.PENDING) == TaskStatus.SUCCESS);
    }
}
