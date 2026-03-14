package com.security.zap.job;

import com.security.zap.entity.Endpoint;
import com.security.zap.entity.Report;
import com.security.zap.repository.EndpointRepository;
import com.security.zap.repository.ReportRepository;
import com.security.zap.service.MailService;
import com.security.zap.service.ZapService;
import com.security.zap.utils.HtmlToPdfConverter;
import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
	 * Adds a {@link ScanJob} to the processing queue for execution.
	 * @param job the {@link ScanJob} containing details about the scanning task to enqueue.
	 * @throws RuntimeException if the thread is interrupted while enqueuing the scan job.
	 */
	public void enqueue(ScanJob job) {
		try {
			queue.put(job);
		} catch (InterruptedException e) {
			Sentry.captureException(e);
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while enqueuing scan job", e);
		}
	}

	/**
	 * Continuously processes scan jobs from the queue until interrupted.
	 */
	private void processQueue() {
		while (true) {
			try {
				ScanJob job = queue.take();
				runScan(job);
			} catch (InterruptedException e) {
				Sentry.captureException(e);
				Thread.currentThread().interrupt();
				log.warn("Worker thread interrupted, stopping queue processing", e);
				break;
			} catch (Exception e) {
				Sentry.captureException(e);
				log.error("Unexpected error while processing queue job", e);
			}
		}
	}

	/**
	 * Executes a scan job for a set of endpoints, generates reports, and sends notifications.
	 * @param job the {@link ScanJob} object containing the details of the scan task, such as the endpoint ID and an {@link SseEmitter} for sending progress updates.
	 */
	private void runScan(ScanJob job) {
		SseEmitter emitter = job.emitter();
		List<MailService.ReportAttachment> attachments = new ArrayList<>();

		AtomicBoolean cancelled = new AtomicBoolean(false);
		emitter.onCompletion(() -> cancelled.set(true));
		emitter.onTimeout(() -> cancelled.set(true));
		emitter.onError(e -> cancelled.set(true));

		try {
			sendEventSafe(emitter, "info", "Scan started");

			List<Endpoint> endpoints = getEndpoints(job);
			if (endpoints.isEmpty()) {
				sendEventSafe(emitter, "done", "No endpoints found");
				return;
			}

			for (Endpoint endpoint : endpoints) {
				if (cancelled.get()) {
					log.warn("Client disconnected, aborting scan for {}", endpoint.getUrl());
					break;
				}
				attachments.add(scanEndpoint(endpoint, emitter, cancelled));
			}

			if (!cancelled.get()) {

				sendEventSafe(emitter, "done", "All endpoints completed");

				try {
					mailService.sendPdfReports(attachments);
					log.info("Email with {} reports sent successfully", attachments.size());
				} catch (Exception e) {
					Sentry.captureException(e);
					log.error("Error sending email with PDF reports", e);
				}
				emitter.complete();
			}

		} catch (Exception e) {
			Sentry.captureException(e);
			log.error("Error while executing scan job", e);
			sendEventSafe(emitter, "error", "Error: " + e.getMessage());
			emitter.completeWithError(e);
		}
	}

	/**
	 * Scans the given endpoint for vulnerabilities, generates a report, and returns an email attachment containing the report.
	 * @param endpoint the {@link Endpoint} to be scanned; contains details such as the URL to be checked.
	 * @param emitter the {@link SseEmitter} used to send progress updates to the client during the scan.
	 * @param cancelled an {@link AtomicBoolean} flag indicating whether the scan should be stopped; if set to true, the scan will be interrupted.
	 * @return a {@link MailService.ReportAttachment} containing the filename and binary content of the generated PDF report for the endpoint.
	 * @throws Exception if an error occurs during the scanning process, report generation, or interaction with external services.
	 */
	private MailService.ReportAttachment scanEndpoint(Endpoint endpoint, SseEmitter emitter, AtomicBoolean cancelled)
			throws Exception {
		sendEventSafe(emitter, "progress", "Preparing scan for " + endpoint.getUrl());
		zapService.prepareSession(endpoint);

		String scanId = zapService.startScan(endpoint.getUrl());
		sendEventSafe(emitter, "progress", "Scanning " + endpoint.getUrl());

		int progress = 0;
		while (progress < 100) {

			if (cancelled.get()) {
				log.warn("Client disconnected, stopping poll for {}", endpoint.getUrl());
				throw new InterruptedException("Client disconnected");
			}

			progress = zapService.getScanProgress(scanId);
			sendEventSafe(emitter, "progress", endpoint.getUrl() + " -> " + progress + "%");
			if (progress < 100) {
				Thread.sleep(SCAN_POLL_INTERVAL_MS);
			}
		}

		sendEventSafe(emitter, "progress", "Generating report for " + endpoint.getUrl());
		String reportHtml = zapService.getHtmlReport();

		byte[] reportPdf = HtmlToPdfConverter.convert(reportHtml);
		Report report = createReport(endpoint, reportPdf);
		reportRepository.save(report);

		sendEventSafe(emitter, "result", "Completed: " + endpoint.getUrl());
		return new MailService.ReportAttachment(report.getFilename(), reportPdf);
	}

	/**
	 * Creates a report for the specified {@link Endpoint} using the given PDF content.
	 * @param endpoint the {@link Endpoint} for which the report is being generated; its details are used to set the report's associated endpoint and filename.
	 * @param pdfContent a byte array representing the binary content of the PDF report.
	 * @return a {@link Report} object populated with the provided endpoint details, execution timestamp, filename, content type, and PDF content.
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
	 * Sends a server-sent event (SSE) to the provided {@link SseEmitter} with a specific event name and data.
	 * @param emitter the {@link SseEmitter} used to send the event to the client.
	 * @param name the name of the event to be sent.
	 * @param data the data payload associated with the event.
	 */
	private void sendEventSafe(SseEmitter emitter, String name, String data) {
		try {
			emitter.send(SseEmitter.event().name(name).data(data));
		} catch (Exception e) {
			Sentry.captureException(e);
			log.warn("Failed to send SSE event [{}: {}]", name, data, e);
		}
	}

	/**
	 * Retrieves a list of endpoints associated with the specified scan job.
	 * @param job the {@link ScanJob} containing the endpoint ID to look up; if the ID is null, all endpoints are retrieved.
	 * @return a list of {@link Endpoint} objects associated with the scan job, or all available endpoints if none are found for the given ID.
	 */
	private List<Endpoint> getEndpoints(ScanJob job) {
		return Optional.ofNullable(job.endpointId())
				.flatMap(endpointRepository::findById)
				.map(List::of)
				.orElseGet(endpointRepository::findAll);
	}
}
