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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class ReportControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ReportRepository reportRepository;

	@InjectMocks
	private ReportController reportController;

	private Report report;

	private static final String DATE_STR = "2025-09-08";
	private static final String TIME_STR = "10:00:00";
	private static final int ENDPOINT_ID = 1;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();

		Endpoint endpoint = new Endpoint();
		endpoint.setId(ENDPOINT_ID);
		endpoint.setName("Endpoint 1");
		endpoint.setUrl("/api/test");
		endpoint.setHttpMethod("GET");

		report = new Report();
		report.setId(1);
		report.setEndpoint(endpoint);
		report.setExecutedAt(Instant.parse(DATE_STR + "T" + TIME_STR + "Z"));
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

	/**
	 * Calculates the start of the day in UTC for a given date.
	 * @return An {@code Instant} representing the beginning of the day (00:00:00) in UTC for the date specified in {@code ReportControllerTest.DATE_STR}.
	 */
	private Instant startOfDay() {
		return LocalDate.parse(ReportControllerTest.DATE_STR)
				.atStartOfDay(ZoneOffset.UTC)
				.toInstant();
	}

	/**
	 * Calculates the end of the day in UTC for a given date.
	 * @return An {@code Instant} representing the end of the day (00:00:00 of the next day) in UTC for the date specified in {@code ReportControllerTest.DATE_STR}.
	 */
	private Instant endOfDay() {
		return LocalDate.parse(ReportControllerTest.DATE_STR)
				.plusDays(1)
				.atStartOfDay(ZoneOffset.UTC)
				.toInstant();
	}

	@Test
	void testDownloadReportByDate_Found() throws Exception {
		when(reportRepository.findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, startOfDay(), endOfDay()))
				.thenReturn(List.of(report));

		mockMvc.perform(get("/scan/reports")
						.param("endpoint_id", String.valueOf(ENDPOINT_ID))
						.param("date", DATE_STR))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\""))
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(content().bytes(report.getReport()));

		verify(reportRepository).findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, startOfDay(), endOfDay());
	}

	@Test
	void testDownloadReportByDate_NotFound() throws Exception {
		when(reportRepository.findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, startOfDay(), endOfDay()))
				.thenReturn(Collections.emptyList());

		mockMvc.perform(get("/scan/reports")
						.param("endpoint_id", String.valueOf(ENDPOINT_ID))
						.param("date", DATE_STR))
				.andExpect(status().isNotFound());

		verify(reportRepository).findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, startOfDay(), endOfDay());
	}

	@Test
	void testDownloadReportByDateAndTime_Found() throws Exception {
		Instant start =
				LocalDate.parse(DATE_STR).atTime(LocalTime.parse(TIME_STR)).toInstant(ZoneOffset.UTC);
		Instant end = start.plusSeconds(1);

		when(reportRepository.findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, start, end))
				.thenReturn(List.of(report));

		mockMvc.perform(get("/scan/reports")
						.param("endpoint_id", String.valueOf(ENDPOINT_ID))
						.param("date", DATE_STR)
						.param("time", TIME_STR))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\""))
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(content().bytes(report.getReport()));

		verify(reportRepository).findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, start, end);
	}

	@Test
	void testDownloadReportByDateAndTime_NotFound() throws Exception {
		Instant start =
				LocalDate.parse(DATE_STR).atTime(LocalTime.parse(TIME_STR)).toInstant(ZoneOffset.UTC);
		Instant end = start.plusSeconds(1);

		when(reportRepository.findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, start, end))
				.thenReturn(Collections.emptyList());

		mockMvc.perform(get("/scan/reports")
						.param("endpoint_id", String.valueOf(ENDPOINT_ID))
						.param("date", DATE_STR)
						.param("time", TIME_STR))
				.andExpect(status().isNotFound());

		verify(reportRepository).findByEndpointIdAndExecutedAtBetween(ENDPOINT_ID, start, end);
	}
}
