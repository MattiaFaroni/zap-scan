package com.security.zap.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.security.zap.entity.Endpoint;
import com.security.zap.entity.Report;
import com.security.zap.repository.EndpointRepository;
import com.security.zap.repository.ReportRepository;
import com.security.zap.service.MailService;
import com.security.zap.service.ZapService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ScanQueueTest {

	@Mock
	private EndpointRepository endpointRepository;

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private ZapService zapService;

	@Mock
	private MailService mailService;

	private ScanQueue scanQueue;
	private Endpoint endpoint;

	@BeforeEach
	void setUp() {
		scanQueue = new ScanQueue(endpointRepository, reportRepository, zapService, mailService);

		endpoint = new Endpoint();
		endpoint.setId(1);
		endpoint.setName("TestEndpoint");
		endpoint.setUrl("https://example.com");
		endpoint.setHttpMethod("GET");
	}

	/**
	 * Invokes the private {@code runScan} method of the {@code ScanQueue} class using reflection.
	 * @param job the {@code ScanJob} instance to be processed by the {@code runScan} method
	 * @throws Exception if the reflection process fails or the invoked method throws an exception
	 */
	private void invokeRunScan(ScanJob job) throws Exception {
		Method runScan = ScanQueue.class.getDeclaredMethod("runScan", ScanJob.class);
		runScan.setAccessible(true);
		runScan.invoke(scanQueue, job);
	}

	/**
	 * Retrieves the internal queue of the {@code ScanQueue} using reflection.
	 * @return the internal {@code BlockingQueue<ScanJob>} of the {@code ScanQueue} instance.
	 * @throws Exception if the field access or reflection process fails.
	 */
	@SuppressWarnings("unchecked")
	private BlockingQueue<ScanJob> getInternalQueue() throws Exception {
		Field queueField = ScanQueue.class.getDeclaredField("queue");
		queueField.setAccessible(true);
		return (BlockingQueue<ScanJob>) queueField.get(scanQueue);
	}

	@Test
	void testRunScan_SingleEndpoint_Success() throws Exception {
		when(endpointRepository.findById(1)).thenReturn(Optional.of(endpoint));
		when(zapService.startScan("https://example.com")).thenReturn("scan123");
		when(zapService.getScanProgress("scan123")).thenReturn(100);
		when(zapService.getHtmlReport()).thenReturn("<html><body>report</body></html>");

		invokeRunScan(new ScanJob(1, new SseEmitter()));

		verify(zapService).prepareSession(endpoint);
		verify(zapService).startScan("https://example.com");
		verify(zapService).getScanProgress("scan123");
		verify(zapService).getHtmlReport();
		verify(reportRepository).save(any(Report.class));
		verify(mailService).sendPdfReports(anyList());
	}

	@Test
	void testRunScan_AllEndpoints_WhenNoIdProvided() throws Exception {
		Endpoint endpoint2 = new Endpoint();
		endpoint2.setId(2);
		endpoint2.setName("TestEndpoint2");
		endpoint2.setUrl("https://example2.com");
		endpoint2.setHttpMethod("POST");

		when(endpointRepository.findAll()).thenReturn(List.of(endpoint, endpoint2));
		when(zapService.startScan("https://example.com")).thenReturn("scan1");
		when(zapService.startScan("https://example2.com")).thenReturn("scan2");
		when(zapService.getScanProgress("scan1")).thenReturn(100);
		when(zapService.getScanProgress("scan2")).thenReturn(100);
		when(zapService.getHtmlReport()).thenReturn("<html><body>report</body></html>");

		invokeRunScan(new ScanJob(null, new SseEmitter()));

		verify(zapService, times(2)).prepareSession(any(Endpoint.class));
		verify(reportRepository, times(2)).save(any(Report.class));
		verify(mailService).sendPdfReports(argThat(list -> list.size() == 2));
	}

	@Test
	void testRunScan_NoEndpointsFound() throws Exception {
		when(endpointRepository.findById(99)).thenReturn(Optional.empty());
		SseEmitter emitter = spy(new SseEmitter());

		invokeRunScan(new ScanJob(99, emitter));

		verify(zapService, never()).prepareSession(any());
		verify(zapService, never()).startScan(any());
		verify(reportRepository, never()).save(any());
		verify(mailService, never()).sendPdfReports(any());
	}

	@Test
	void testRunScan_ZapServiceThrows_MailNotSent() throws Exception {
		when(endpointRepository.findById(1)).thenReturn(Optional.of(endpoint));
		when(zapService.startScan(anyString())).thenThrow(new RuntimeException("ZAP down"));

		invokeRunScan(new ScanJob(1, new SseEmitter()));

		verify(mailService, never()).sendPdfReports(any());
		verify(reportRepository, never()).save(any());
	}

	@Test
	void testEnqueue_AddsJobToQueue() throws Exception {
		ScanJob job = new ScanJob(1, new SseEmitter());

		scanQueue.enqueue(job);

		BlockingQueue<ScanJob> queue = getInternalQueue();
		assertEquals(1, queue.size());
		assertSame(job, queue.peek());
	}

	@Test
	void testEnqueue_MultipleJobs_PreservesOrder() throws Exception {
		ScanJob job1 = new ScanJob(1, new SseEmitter());
		ScanJob job2 = new ScanJob(2, new SseEmitter());
		ScanJob job3 = new ScanJob(null, new SseEmitter());

		scanQueue.enqueue(job1);
		scanQueue.enqueue(job2);
		scanQueue.enqueue(job3);

		BlockingQueue<ScanJob> queue = getInternalQueue();
		assertEquals(3, queue.size());
		assertSame(job1, queue.poll());
		assertSame(job2, queue.poll());
		assertSame(job3, queue.poll());
	}
}
