package com.forge.job.application.handler;

import com.forge.job.domain.Job;
import com.forge.job.domain.JobType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class JobHandlerRegistry {
    private final Map<JobType, JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> registeredHandlers) {
        handlers = new EnumMap<>(JobType.class);
        for (JobHandler handler : registeredHandlers) {
            JobHandler previous = handlers.put(handler.supportedType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate handler for " + handler.supportedType());
            }
        }
    }

    public JobHandler handlerFor(Job job) {
        JobHandler handler = handlers.get(job.getType());
        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + job.getType());
        }
        return handler;
    }
}