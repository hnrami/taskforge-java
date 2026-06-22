package com.taskforge.handler;

import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Executes a shell command in a subprocess.
 *
 * Required config keys:
 *   command (String, required) — the shell command to run
 *
 * Success: exit code 0.
 * Failure: non-zero exit code (non-retryable unless transient OS error).
 *
 * Output keys: stdout, stderr, exitCode
 */
@Component
public class ScriptTaskHandler implements TaskHandler {

    private final AtomicReference<Process> runningProcess = new AtomicReference<>(null);

    @Override
    public String type() {
        return "script";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        String command = (String) context.getTaskConfig().get("command");
        if (command == null || command.isBlank()) {
            return failure("Missing required config: command", false);
        }

        ProcessBuilder pb = buildProcess(command);
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();
            runningProcess.set(process);

            String stdout = readStream(new BufferedReader(new InputStreamReader(process.getInputStream())));
            String stderr = readStream(new BufferedReader(new InputStreamReader(process.getErrorStream())));

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return failure("Script timed out", true);
            }

            int exitCode = process.exitValue();
            runningProcess.set(null);

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("stdout", stdout);
            outputs.put("stderr", stderr);
            outputs.put("exitCode", exitCode);

            if (exitCode == 0) {
                return success(outputs);
            } else {
                return failure("Script exited with code " + exitCode + ". stderr: " + stderr, false);
            }

        } catch (IOException e) {
            return failure("Failed to start script: " + e.getMessage(), true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("Script execution interrupted", true);
        }
    }

    @Override
    public void cancel() {
        Process p = runningProcess.getAndSet(null);
        if (p != null && p.isAlive()) {
            p.destroy();
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
            }
        }
    }

    // -------------------------------------------------------------------------

    private ProcessBuilder buildProcess(String command) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            return new ProcessBuilder("sh", "-c", command);
        }
    }

    private String readStream(BufferedReader reader) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }

    private TaskResult success(Map<String, Object> outputs) {
        TaskResult r = new TaskResult();
        r.setSuccess(true);
        r.setOutputs(outputs);
        return r;
    }

    private TaskResult failure(String error, boolean retryable) {
        TaskResult r = new TaskResult();
        r.setSuccess(false);
        r.setErrorMessage(error);
        r.setRetryable(retryable);
        return r;
    }
}
