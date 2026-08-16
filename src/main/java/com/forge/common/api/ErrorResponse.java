package com.forge.common.api;

import java.time.Instant;
import java.util.List;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ErrorResponse {
    public Instant timestamp = Instant.now();
    public int status;
    public String code;
    public String message;
    public String path;
    public String traceId;
    public List<String> details;


    public ErrorResponse(int status, String code, String message, String path, String traceId, List<String> details) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.details = details;
    }
}
