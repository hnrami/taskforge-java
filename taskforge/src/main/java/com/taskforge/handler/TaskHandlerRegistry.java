package com.taskforge.handler;

import com.taskforge.exception.WorkflowValidationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class TaskHandlerRegistry {

    private final Map<String, TaskHandler> registry = new HashMap<>();

    /**
     * ObjectProvider is used instead of List<TaskHandler> so Spring does not throw
     * NoSuchBeanDefinitionException when no handlers are registered yet.
     */
    public TaskHandlerRegistry(ObjectProvider<TaskHandler> handlerProvider) {
        handlerProvider.forEach(handler -> registry.put(handler.type(), handler));
    }

    public TaskHandler get(String type) {
        TaskHandler handler = registry.get(type);
        if (handler == null) {
            throw new WorkflowValidationException(
                    "No handler registered for task type: '" + type + "'. Registered types: " + registry.keySet());
        }
        return handler;
    }

    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(registry.keySet());
    }
}

