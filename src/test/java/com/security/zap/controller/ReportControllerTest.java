package com.security.zap.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.security.zap.entity.Endpoint;
import com.security.zap.entity.Report;
import com.security.zap.repository.ReportRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ReportControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ReportRepository reportRepository;

	@InjectMocks
	private ReportController reportController;

	private Report report;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();

		Endpoint endpoint = new Endpoint();
		endpoint.setId(1);
		endpoint.setName("Endpoint 1");
		endpoint.setUrl("/api/test");
		endpoint.setHttpMethod("GET");

		report = new Report();
		report.setId(1);
		report.setEndpoint(endpoint);
		report.setExecutedAt(Instant.parse("2025-09-08T10:00:00Z"));
		report.setFilename("report.pdf");
		report.setContentType(MediaType.APPLICATION_PDF_VALUE);
		report.setReport("Test PDF Content".getBytes());
	}

	@Test
	void testDownloadReportByDateFound() throws Exception {
		Instant startOfDay =
				LocalDate.parse("2025-09-08").atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = LocalDate.parse("2025-09-08")
				.plusDays(1)
				.atStartOfDay(ZoneOffset.UTC)
				.toInstant();

		when(reportRepository.findByEndpointIdAndExecutedAtBetween(1, startOfDay, endOfDay))
				.thenReturn(Collections.singletonList(report));

		mockMvc.perform(get("/scan/reports").param("endpoint_id", "1").param("date", "2025-09-08"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\""))
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(content().bytes(report.getReport()));

		verify(reportRepository, times(1)).findByEndpointIdAndExecutedAtBetween(1, startOfDay, endOfDay);
	}

	@Test
	void testDownloadReportByDateNotFound() throws Exception {
		Instant startOfDay =
				LocalDate.parse("2025-09-08").atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant endOfDay = LocalDate.parse("2025-09-08")
				.plusDays(1)
				.atStartOfDay(ZoneOffset.UTC)
				.toInstant();

		when(reportRepository.findByEndpointIdAndExecutedAtBetween(1, startOfDay, endOfDay))
				.thenReturn(Collections.emptyList());

		mockMvc.perform(get("/scan/reports").param("endpoint_id", "1").param("date", "2025-09-08"))
				.andExpect(status().isNotFound());

		verify(reportRepository, times(1)).findByEndpointIdAndExecutedAtBetween(1, startOfDay, endOfDay);
	}

	@Test
	void testDownloadReportByDateAndTimeFound() throws Exception {
		LocalDate date = LocalDate.parse("2025-09-08");
		LocalTime time = LocalTime.parse("10:00:00");
		Instant start = date.atTime(time).toInstant(ZoneOffset.UTC);
		Instant end = date.atTime(time).plusSeconds(1).toInstant(ZoneOffset.UTC);

		when(reportRepository.findByEndpointIdAndExecutedAtBetween(1, start, end))
				.thenReturn(Collections.singletonList(report));

		mockMvc.perform(get("/scan/reports")
						.param("endpoint_id", "1")
						.param("date", "2025-09-08")
						.param("time", "10:00:00"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\""))
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(content().bytes(report.getReport()));

		verify(reportRepository, times(1)).findByEndpointIdAndExecutedAtBetween(1, start, end);
	}
}
