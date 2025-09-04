package com.security.zap.controller;

import com.security.zap.job.ScanJob;
import com.security.zap.job.ScanQueue;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ScanController {

	private final ScanQueue scanQueue;

	public ScanController(ScanQueue scanQueue) {
		this.scanQueue = scanQueue;
	}

	@GetMapping(value = "/scan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamScan(@RequestParam(name = "endpoint_id", required = false) Integer endpointId) {
		SseEmitter emitter = new SseEmitter(0L);
		ScanJob job = new ScanJob(endpointId, emitter);
		scanQueue.enqueue(job);
		return emitter;
	}
}
