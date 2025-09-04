package com.security.zap.job;

import com.security.zap.entity.Endpoint;
import com.security.zap.entity.Report;
import com.security.zap.repository.EndpointRepository;
import com.security.zap.repository.ReportRepository;
import com.security.zap.service.MailService;
import com.security.zap.service.ZapService;
import com.security.zap.utils.HtmlToPdfConverter;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Log4j2
public class ScanQueue {

	private static final int SCAN_POLL_INTERVAL_MS = 2000;

	private final BlockingQueue<ScanJob> queue = new LinkedBlockingQueue<>();

	private final EndpointRepository endpointRepository;
	private final ReportRepository reportRepository;
	private final ZapService zapService;
	private final MailService mailService;

	public ScanQueue(
			EndpointRepository endpointRepository,
			ReportRepository reportRepository,
			ZapService zapService,
			MailService mailService) {
		this.endpointRepository = endpointRepository;
		this.reportRepository = reportRepository;
		this.zapService = zapService;
		this.mailService = mailService;
	}

	@PostConstruct
	public void startWorker() {
		Thread worker = new Thread(this::processQueue, "scan-worker");
		worker.setDaemon(true);
		worker.start();
		log.info("Scan worker thread started");
	}

	/**
	 * Adds a {@link ScanJob} to the queue for processing.
	 * @param job the scan job to be added to the queue; it contains the endpoint ID
	 *            and an associated {@link SseEmitter} for streaming scan events.
	 * @throws RuntimeException if the thread is interrupted during the enqueue action.
	 */
	public void enqueue(ScanJob job) {
		try {
			queue.put(job);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while enqueuing scan job", e);
		}
	}

	/**
	 * Continuously processes scan jobs from the queue.
	 * The method retrieves jobs from a thread-safe queue in a blocking manner, ensuring
	 * that jobs are processed in the order they are added to the queue. Each job is
	 * executed by invoking the {@code runScan(ScanJob)} method.
	 */
	private void processQueue() {
		while (true) {
			try {
				ScanJob job = queue.take();
				runScan(job);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Worker thread interrupted, stopping queue processing", e);
				break;
			} catch (Exception e) {
				log.error("Unexpected error while processing queue job", e);
			}
		}
	}

	/**
	 * Executes a security scan for the specified {@link ScanJob}.
	 * @param job the {@link ScanJob} containing the endpoint ID and {@link SseEmitter}
	 *            for real-time streaming of scan progress.
	 */
	private void runScan(ScanJob job) {
		SseEmitter emitter = job.emitter();
		List<MailService.ReportAttachment> attachments = new ArrayList<>();

		try {
			sendEventSafe(emitter, "info", "Scan started");

			List<Endpoint> endpoints = getEndpoints(job);
			if (endpoints.isEmpty()) {
				sendEventSafe(emitter, "done", "No endpoints found");
				return;
			}

			for (Endpoint endpoint : endpoints) {
				attachments.add(scanEndpoint(endpoint, emitter));
			}

			sendEventSafe(emitter, "done", "All endpoints completed");

			try {
				mailService.sendPdfReports(attachments);
				log.info("Email with {} reports sent successfully", attachments.size());
			} catch (Exception e) {
				log.error("Error sending email with PDF reports", e);
			}

		} catch (Exception e) {
			log.error("Error while executing scan job", e);
			sendEventSafe(emitter, "error", "Error: " + e.getMessage());
			emitter.completeWithError(e);
		} finally {
			emitter.complete();
		}
	}

	/**
	 * Performs a security scan on the specified endpoint and streams progress updates to the provided emitter.
	 * @param endpoint the {@link Endpoint} object representing the target endpoint to be scanned
	 * @param emitter the {@link SseEmitter} used to stream progress updates during the scan
	 * @return a {@link MailService.ReportAttachment} object containing the filename and PDF content of the generated report
	 * @throws Exception if any error occurs during the scanning or report generation process
	 */
	private MailService.ReportAttachment scanEndpoint(Endpoint endpoint, SseEmitter emitter) throws Exception {

		if (!zapService.isUrlKnown(endpoint.getUrl())) {
			zapService.accessUrl(endpoint.getUrl());
			Thread.sleep(2000);
		}

		if (zapService.isScanRunning()) {
			sendEventSafe(emitter, "info", "Another scan is currently running, waiting...");
			while (zapService.isScanRunning()) {
				Thread.sleep(2000);
			}
		}

		sendEventSafe(emitter, "progress", "Scanning " + endpoint.getUrl());

		zapService.newSession("session-" + endpoint.getId(), true);
		zapService.accessUrl(endpoint.getUrl());
		String scanId = zapService.startScan(endpoint.getUrl());

		while (zapService.getScanProgress(scanId) < 100) {
			int progress = zapService.getScanProgress(scanId);
			sendEventSafe(emitter, "progress", endpoint.getUrl() + " -> " + progress + "%");
			Thread.sleep(SCAN_POLL_INTERVAL_MS);
		}

		String reportHtml = zapService.getHtmlReport();
		byte[] reportPdf = HtmlToPdfConverter.convert(reportHtml);

		Report report = createReport(endpoint, reportPdf);
		reportRepository.save(report);

		sendEventSafe(emitter, "result", "Completed: " + endpoint.getUrl());

		return new MailService.ReportAttachment(report.getFilename(), reportPdf);
	}

	/**
	 * Creates a new {@link Report} object associated with the provided {@link Endpoint} and containing the supplied PDF content.
	 * @param endpoint the {@link Endpoint} object to which the report is linked
	 * @param pdfContent the PDF content of the report as a byte array
	 * @return a {@link Report} object initialized with the provided endpoint and PDF content
	 */
	private Report createReport(Endpoint endpoint, byte[] pdfContent) {
		Report report = new Report();
		report.setEndpoint(endpoint);
		report.setExecutedAt(Instant.now());
		report.setFilename(endpoint.getName().replace(" ", "_") + "-"
				+ LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf");
		report.setContentType("application/pdf");
		report.setReport(pdfContent);
		return report;
	}

	/**
	 * Sends a Server-Sent Event (SSE) to the specified {@link SseEmitter} with the given event name and data.
	 * @param emitter the {@link SseEmitter} to which the SSE event is sent
	 * @param name the name of the SSE event
	 * @param data the data to include in the SSE event
	 */
	private void sendEventSafe(SseEmitter emitter, String name, String data) {
		try {
			emitter.send(SseEmitter.event().name(name).data(data));
		} catch (Exception e) {
			log.warn("Failed to send SSE event [{}: {}]", name, data, e);
		}
	}

	/**
	 * Retrieves a list of {@link Endpoint} objects associated with the given {@link ScanJob}.
	 * @param job the {@link ScanJob} containing the ID of the endpoint to retrieve
	 * @return a list of {@link Endpoint} objects; either a single-element list if the endpoint ID
	 *         exists, or all endpoints from the repository if no specific ID is provided
	 */
	private List<Endpoint> getEndpoints(ScanJob job) {
		return Optional.ofNullable(job.endpointId())
				.flatMap(endpointRepository::findById)
				.map(List::of)
				.orElseGet(endpointRepository::findAll);
	}
}
