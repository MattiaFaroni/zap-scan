package com.security.zap.controller;

import com.security.zap.entity.Endpoint;
import com.security.zap.repository.EndpointRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scan")
public class EndpointController {

	private final EndpointRepository endpointRepository;

	public EndpointController(EndpointRepository endpointRepository) {
		this.endpointRepository = endpointRepository;
	}

	@GetMapping("/endpoints")
	public ResponseEntity<?> getEndpoints(@RequestParam(value = "id", required = false) Integer id) {
		if (id == null) {
			List<Endpoint> endpoints = endpointRepository.findAll();
			return ResponseEntity.ok(endpoints);
		} else {
			return endpointRepository.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound()
					.build());
		}
	}
}
