package com.forge.job.api;

import com.forge.job.api.dto.*;
import com.forge.job.application.JobService;
import com.forge.job.domain.Job;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submit(@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody SubmitJobRequest request) {
        var job = service.submit(key, request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.getId().value())).body(toResponse(job));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return toResponse(service.get(id));
    }

    @PostMapping("/{id}/cancel")
    public JobResponse cancel(@PathVariable UUID id) {
        return toResponse(service.cancel(id));
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(job.getId().toString(), job.getType(), job.getTenantId().toString(), job.getPayload(),
                job.getPriority(), job.getStatus(), job.getRetryCount());
    }
}