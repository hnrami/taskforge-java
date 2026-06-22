package com.taskforge.engine;

import com.taskforge.exception.WorkflowValidationException;
import com.taskforge.model.TaskDefinition;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DagValidatorTest {

    private final DagValidator validator = new DagValidator();
    private final Set<String> validTypes = Set.of("notification", "http", "script", "database", "approval");

    // ------------------------------------------------------------------
    // Valid workflow
    // ------------------------------------------------------------------

    @Test
    void validWorkflow_linearChain_accepted() {
        List<TaskDefinition> tasks = List.of(
                task("A", "notification", List.of()),
                task("B", "notification", List.of("A")),
                task("C", "notification", List.of("B"))
        );
        assertDoesNotThrow(() -> validator.validate(tasks, validTypes));
    }

    @Test
    void validWorkflow_diamondShape_accepted() {
        List<TaskDefinition> tasks = List.of(
                task("A", "notification", List.of()),
                task("B", "notification", List.of("A")),
                task("C", "notification", List.of("A")),
                task("D", "notification", List.of("B", "C"))
        );
        assertDoesNotThrow(() -> validator.validate(tasks, validTypes));
    }

    // ------------------------------------------------------------------
    // Cycle detection
    // ------------------------------------------------------------------

    @Test
    void cycleDetected_throwsWithCyclePath() {
        // A -> B -> C -> A
        List<TaskDefinition> tasks = List.of(
                task("A", "notification", List.of("C")),
                task("B", "notification", List.of("A")),
                task("C", "notification", List.of("B"))
        );
        WorkflowValidationException ex = assertThrows(WorkflowValidationException.class,
                () -> validator.validate(tasks, validTypes));
        assertTrue(ex.getMessage().contains("Cycle detected"),
                "Expected 'Cycle detected' in: " + ex.getMessage());
    }

    @Test
    void selfLoop_detected() {
        List<TaskDefinition> tasks = List.of(
                task("A", "notification", List.of("A"))
        );
        WorkflowValidationException ex = assertThrows(WorkflowValidationException.class,
                () -> validator.validate(tasks, validTypes));
        assertTrue(ex.getMessage().contains("Cycle detected"));
    }

    // ------------------------------------------------------------------
    // Missing dependency
    // ------------------------------------------------------------------

    @Test
    void missingDependency_throwsWithDetails() {
        List<TaskDefinition> tasks = List.of(
                task("B", "notification", List.of("A"))    // A does not exist
        );
        WorkflowValidationException ex = assertThrows(WorkflowValidationException.class,
                () -> validator.validate(tasks, validTypes));
        assertTrue(ex.getMessage().contains("Unknown dependency"),
                "Expected 'Unknown dependency' in: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("A"));
    }

    // ------------------------------------------------------------------
    // Unknown task type
    // ------------------------------------------------------------------

    @Test
    void unknownTaskType_throwsWithTypeName() {
        List<TaskDefinition> tasks = List.of(
                task("A", "unknown-type", List.of())
        );
        WorkflowValidationException ex = assertThrows(WorkflowValidationException.class,
                () -> validator.validate(tasks, validTypes));
        assertTrue(ex.getMessage().contains("Unknown task type"),
                "Expected 'Unknown task type' in: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("unknown-type"));
    }

    // ------------------------------------------------------------------
    // Duplicate IDs
    // ------------------------------------------------------------------

    @Test
    void duplicateTaskId_throwsException() {
        List<TaskDefinition> tasks = List.of(
                task("A", "notification", List.of()),
                task("A", "notification", List.of())
        );
        WorkflowValidationException ex = assertThrows(WorkflowValidationException.class,
                () -> validator.validate(tasks, validTypes));
        assertTrue(ex.getMessage().contains("Duplicate task id"));
    }

    // ------------------------------------------------------------------
    // Empty workflow
    // ------------------------------------------------------------------

    @Test
    void emptyWorkflow_throwsException() {
        assertThrows(WorkflowValidationException.class,
                () -> validator.validate(List.of(), validTypes));
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private TaskDefinition task(String id, String type, List<String> deps) {
        TaskDefinition td = new TaskDefinition();
        td.setId(id);
        td.setType(type);
        td.setDependsOn(new ArrayList<>(deps));
        td.setConfig(new HashMap<>());
        return td;
    }
}
