package com.forge.job.api;

import com.forge.job.api.dto.*;
import com.forge.job.application.JobService;
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
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.getId().value())).body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(service.get(id));
    }

    @PostMapping("/{id}/cancel")
    public JobResponse cancel(@PathVariable UUID id) {
        return JobResponse.from(service.cancel(id));
    }
}