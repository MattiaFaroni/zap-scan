package com.security.zap.repository;

import com.security.zap.entity.Report;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
	List<Report> findByEndpointIdAndExecutedAtBetween(Integer endpointId, Instant start, Instant end);
}
