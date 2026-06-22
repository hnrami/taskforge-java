package com.taskforge.handler;

import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates sending a notification (Email / Slack / webhook).
 *
 * Optional config keys:
 *   channel  (String) — "email", "slack", "webhook" (default: "email")
 *   to       (String) — recipient or channel name
 *   message  (String) — notification body
 *
 * Always succeeds unless required data is missing.
 * Output: deliveryStatus, channel
 */
@Component
public class NotificationTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationTaskHandler.class);

    @Override
    public String type() {
        return "notification";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        Map<String, Object> config = context.getTaskConfig();

        String channel = config.getOrDefault("channel", "email").toString();
        String to      = config.getOrDefault("to", "team@example.com").toString();
        String message = config.getOrDefault("message", "Workflow notification").toString();

        log.info("[NotificationTaskHandler] Sending {} notification to '{}': {}", channel, to, message);

        // Simulated delivery — in production, inject real mail/Slack client here
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("deliveryStatus", "DELIVERED");
        outputs.put("channel", channel);
        outputs.put("recipient", to);

        TaskResult result = new TaskResult();
        result.setSuccess(true);
        result.setOutputs(outputs);
        return result;
    }

    @Override
    public void cancel() {
        // Notification is fire-and-forget; nothing to cancel
    }
}
