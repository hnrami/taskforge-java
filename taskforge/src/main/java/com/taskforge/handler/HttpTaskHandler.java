package com.taskforge.handler;

import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes an outbound HTTP request.
 *
 * Required config keys in TaskContext:
 *   url    (String, required)
 *   method (String, default GET)
 *   body   (String, optional)
 *   headers (Map<String,String>, optional)
 *
 * Success: HTTP 2xx status code.
 * Retryable failure: HTTP 5xx or IOException.
 * Non-retryable failure: HTTP 4xx.
 */
@Component
public class HttpTaskHandler implements TaskHandler {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public String type() {
        return "http";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        cancelled.set(false);

        Map<String, Object> config = context.getTaskConfig();
        String url = (String) config.get("url");
        if (url == null || url.isBlank()) {
            return failure("Missing required config: url", false);
        }

        String method = config.getOrDefault("method", "GET").toString().toUpperCase();
        String body   = config.containsKey("body") ? config.get("body").toString() : null;

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));

        // Apply optional headers
        Object headersObj = config.get("headers");
        if (headersObj instanceof Map<?, ?> headersMap) {
            headersMap.forEach((k, v) -> requestBuilder.header(k.toString(), v.toString()));
        }

        BodyPublishers.ofString("");   // warm up
        HttpRequest.BodyPublisher publisher = (body != null)
                ? BodyPublishers.ofString(body)
                : BodyPublishers.noBody();

        requestBuilder.method(method, publisher);
        HttpRequest request = requestBuilder.build();

        try {
            if (cancelled.get()) {
                return failure("Task cancelled before HTTP request", true);
            }

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("statusCode", status);
            outputs.put("responseBody", response.body());

            if (status >= 200 && status < 300) {
                return success(outputs);
            } else if (status >= 500) {
                return retryableFailure("HTTP " + status + " from " + url, outputs);
            } else {
                return failure("HTTP " + status + " from " + url + " (non-retryable)", false);
            }

        } catch (IOException e) {
            return retryableFailure("IO error calling " + url + ": " + e.getMessage(), new HashMap<>());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("HTTP request interrupted", true);
        }
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    // -------------------------------------------------------------------------

    private TaskResult success(Map<String, Object> outputs) {
        TaskResult result = new TaskResult();
        result.setSuccess(true);
        result.setOutputs(outputs);
        return result;
    }

    private TaskResult failure(String error, boolean retryable) {
        TaskResult result = new TaskResult();
        result.setSuccess(false);
        result.setErrorMessage(error);
        result.setRetryable(retryable);
        return result;
    }

    private TaskResult retryableFailure(String error, Map<String, Object> outputs) {
        TaskResult result = new TaskResult();
        result.setSuccess(false);
        result.setErrorMessage(error);
        result.setRetryable(true);
        result.setOutputs(outputs);
        return result;
    }
}
