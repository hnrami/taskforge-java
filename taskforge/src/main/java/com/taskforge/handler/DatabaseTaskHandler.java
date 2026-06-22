package com.taskforge.handler;

import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates executing a database migration or query.
 *
 * Optional config keys:
 *   operation  (String) — "migration" or "query" (default: "migration")
 *   script     (String) — SQL or migration name to simulate
 *
 * Always succeeds in simulation mode.
 * In production, inject a real DataSource / Flyway / Liquibase client.
 *
 * Output: operation, executionResult
 */
@Component
public class DatabaseTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTaskHandler.class);

    @Override
    public String type() {
        return "database";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        Map<String, Object> config = context.getTaskConfig();

        String operation = config.getOrDefault("operation", "migration").toString();
        String script    = config.getOrDefault("script", "unnamed-script").toString();

        log.info("[DatabaseTaskHandler] Executing {} operation: {}", operation, script);

        // Simulated execution — replace with real DB logic in production
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("operation", operation);
        outputs.put("script", script);
        outputs.put("executionResult", "SUCCESS");

        TaskResult result = new TaskResult();
        result.setSuccess(true);
        result.setOutputs(outputs);
        return result;
    }

    @Override
    public void cancel() {
        // Simulation only — real implementation would cancel open connections
    }
}
