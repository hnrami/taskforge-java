package com.taskforge.engine;

import com.taskforge.exception.WorkflowValidationException;
import com.taskforge.model.TaskDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DagValidator {

    private enum VisitState {
        NOT_VISITED, VISITING, VISITED
    }

    /**
     * Validates the task list of a workflow definition.
     * Throws WorkflowValidationException on:
     *   - missing dependency reference
     *   - cycle in the dependency graph
     *
     * @param tasks ordered list of task definitions
     * @param registeredTypes set of task type strings supported by the handler registry
     */
    public void validate(List<TaskDefinition> tasks, Set<String> registeredTypes) {
        if (tasks == null || tasks.isEmpty()) {
            throw new WorkflowValidationException("Workflow must contain at least one task");
        }

        Map<String, TaskDefinition> taskIndex = buildIndex(tasks);

        validateNoDuplicateIds(tasks);
        validateDependenciesExist(tasks, taskIndex);
        validateTaskTypes(tasks, registeredTypes);
        validateNoCycles(tasks, taskIndex);
    }

    // -------------------------------------------------------------------------
    // Duplicate ID check
    // -------------------------------------------------------------------------

    private void validateNoDuplicateIds(List<TaskDefinition> tasks) {
        Set<String> seen = new HashSet<>();
        for (TaskDefinition task : tasks) {
            if (!seen.add(task.getId())) {
                throw new WorkflowValidationException("Duplicate task id: " + task.getId());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Missing dependency check
    // -------------------------------------------------------------------------

    private void validateDependenciesExist(List<TaskDefinition> tasks, Map<String, TaskDefinition> taskIndex) {
        for (TaskDefinition task : tasks) {
            if (task.getDependsOn() == null) {
                continue;
            }
            for (String dep : task.getDependsOn()) {
                if (!taskIndex.containsKey(dep)) {
                    throw new WorkflowValidationException(
                            "Unknown dependency '" + dep + "' for task '" + task.getId() + "'");
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Task type check
    // -------------------------------------------------------------------------

    private void validateTaskTypes(List<TaskDefinition> tasks, Set<String> registeredTypes) {
        for (TaskDefinition task : tasks) {
            if (!registeredTypes.contains(task.getType())) {
                throw new WorkflowValidationException(
                        "Unknown task type '" + task.getType() + "' for task '" + task.getId()
                                + "'. Registered types: " + registeredTypes);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cycle detection via DFS (NOT_VISITED -> VISITING -> VISITED)
    // -------------------------------------------------------------------------

    private void validateNoCycles(List<TaskDefinition> tasks, Map<String, TaskDefinition> taskIndex) {
        Map<String, VisitState> state = new HashMap<>();
        for (TaskDefinition task : tasks) {
            state.put(task.getId(), VisitState.NOT_VISITED);
        }

        Deque<String> path = new ArrayDeque<>();

        for (TaskDefinition task : tasks) {
            if (state.get(task.getId()) == VisitState.NOT_VISITED) {
                dfs(task.getId(), taskIndex, state, path);
            }
        }
    }

    private void dfs(String taskId,
                     Map<String, TaskDefinition> taskIndex,
                     Map<String, VisitState> state,
                     Deque<String> path) {

        state.put(taskId, VisitState.VISITING);
        path.addLast(taskId);

        TaskDefinition task = taskIndex.get(taskId);
        List<String> deps = task.getDependsOn();

        if (deps != null) {
            for (String dep : deps) {
                VisitState depState = state.get(dep);
                if (depState == VisitState.VISITING) {
                    // Reconstruct the cycle path for a clear error message
                    List<String> cycle = new ArrayList<>(path);
                    int cycleStart = cycle.indexOf(dep);
                    List<String> cycleSegment = cycle.subList(cycleStart, cycle.size());
                    cycleSegment.add(dep);
                    throw new WorkflowValidationException(
                            "Cycle detected: " + String.join(" -> ", cycleSegment));
                }
                if (depState == VisitState.NOT_VISITED) {
                    dfs(dep, taskIndex, state, path);
                }
            }
        }

        path.removeLast();
        state.put(taskId, VisitState.VISITED);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Map<String, TaskDefinition> buildIndex(List<TaskDefinition> tasks) {
        Map<String, TaskDefinition> index = new LinkedHashMap<>();
        for (TaskDefinition task : tasks) {
            index.put(task.getId(), task);
        }
        return index;
    }
}
