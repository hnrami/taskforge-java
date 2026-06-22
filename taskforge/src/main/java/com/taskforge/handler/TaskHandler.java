package com.taskforge.handler;

import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;

public interface TaskHandler {

    String type();

    TaskResult execute(TaskContext context);

    void cancel();
}
