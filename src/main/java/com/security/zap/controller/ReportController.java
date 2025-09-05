package com.security.zap.controller;

import com.security.zap.repository.ReportRepository;
import java.time.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scan")
public class ReportController {

	private final ReportRepository reportRepository;

	public ReportController(ReportRepository reportRepository) {
		this.reportRepository = reportRepository;
	}

	@GetMapping("/reports")
	public ResponseEntity<byte[]> downloadReport(
			@RequestParam("endpoint_id") Integer endpointId,
			@RequestParam("date") String date,
			@RequestParam(value = "time", required = false) String time) {
		LocalDate localDate = LocalDate.parse(date);

		if (time == null) {
			Instant startOfDay = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
			Instant endOfDay =
					localDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

			return reportRepository.findByEndpointIdAndExecutedAtBetween(endpointId, startOfDay, endOfDay).stream()
					.findFirst()
					.map(report -> ResponseEntity.ok()
							.header(
									HttpHeaders.CONTENT_DISPOSITION,
									"attachment; filename=\"" + report.getFilename() + "\"")
							.contentType(MediaType.APPLICATION_PDF)
							.body(report.getReport()))
					.orElseGet(() -> ResponseEntity.notFound().build());

		} else {
			LocalTime localTime = LocalTime.parse(time);
			LocalDateTime ldt = localDate.atTime(localTime);

			Instant start = ldt.toInstant(ZoneOffset.UTC);
			Instant end = ldt.plusSeconds(1).toInstant(ZoneOffset.UTC);

			return reportRepository.findByEndpointIdAndExecutedAtBetween(endpointId, start, end).stream()
					.findFirst()
					.map(report -> ResponseEntity.ok()
							.header(
									HttpHeaders.CONTENT_DISPOSITION,
									"attachment; filename=\"" + report.getFilename() + "\"")
							.contentType(MediaType.APPLICATION_PDF)
							.body(report.getReport()))
					.orElseGet(() -> ResponseEntity.notFound().build());
		}
	}
}
