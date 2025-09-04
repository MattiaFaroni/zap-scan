package com.security.zap.job;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record ScanJob(Integer endpointId, SseEmitter emitter) {}
