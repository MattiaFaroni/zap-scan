package com.security.zap.health;

import com.security.zap.service.ZapService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ZapHealthIndicator implements HealthIndicator {

	private final ZapService zapService;

	public ZapHealthIndicator(ZapService zapService) {
		this.zapService = zapService;
	}

	@Override
	public Health health() {
		if (zapService.isZapRunning()) {
			return Health.up().withDetail("ZAP", "Running").build();
		} else {
			return Health.down().withDetail("ZAP", "Not reachable").build();
		}
	}
}
