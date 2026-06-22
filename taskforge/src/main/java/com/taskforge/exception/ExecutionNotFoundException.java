package com.taskforge.exception;

public class ExecutionNotFoundException extends RuntimeException {

    public ExecutionNotFoundException(String id) {
        super("Execution not found: " + id);
    }
}
