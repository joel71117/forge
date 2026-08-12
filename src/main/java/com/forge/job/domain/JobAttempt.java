package com.forge.job.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class JobAttempt {
    private final JobAttemptId id;
    private final UUID jobId;
    private final UUID workerId;
    private final int attemptNumber;
    private final Instant startedAt;
    private Instant finishedAt;
    private String status;
    private String errorCode;
    private String errorMessage;

    public JobAttempt(UUID jobId, UUID workerId, int attemptNumber, Instant startedAt, Instant finishedAt,
                      String status, String errorCode, String errorMessage) {
        this.id = new JobAttemptId(UUID.randomUUID());
        this.jobId = jobId;
        this.workerId = workerId;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
