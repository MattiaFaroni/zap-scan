package com.security.zap.job;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.security.zap.entity.Endpoint;
import com.security.zap.entity.Report;
import com.security.zap.repository.EndpointRepository;
import com.security.zap.repository.ReportRepository;
import com.security.zap.service.MailService;
import com.security.zap.service.ZapService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

	@BeforeEach
	void setUp() {
		scanQueue = new ScanQueue(endpointRepository, reportRepository, zapService, mailService);
	}

	@Test
	void testRunScanSuccess() throws Exception {
		Endpoint endpoint = new Endpoint();
		endpoint.setId(1);
		endpoint.setName("TestEndpoint");
		endpoint.setUrl("https://example.com");

		when(endpointRepository.findById(1)).thenReturn(Optional.of(endpoint));
		when(zapService.isUrlKnown(anyString())).thenReturn(true);
		when(zapService.isScanRunning()).thenReturn(false);
		when(zapService.startScan(anyString())).thenReturn("scan123");
		when(zapService.getScanProgress("scan123")).thenReturn(100);
		when(zapService.getHtmlReport()).thenReturn("<html>report</html>");

		ScanJob job = new ScanJob(1, new SseEmitter());

		Method runScan = ScanQueue.class.getDeclaredMethod("runScan", ScanJob.class);
		runScan.setAccessible(true);
		runScan.invoke(scanQueue, job);

		verify(reportRepository).save(any(Report.class));
		verify(mailService).sendPdfReports(anyList());
		verify(zapService).newSession(startsWith("session-"), eq(true));
	}

	@Test
	void testEnqueueAddsJob() throws NoSuchFieldException, IllegalAccessException {
		ScanJob job = new ScanJob(1, new SseEmitter());

		scanQueue.enqueue(job);

		Field queueField = ScanQueue.class.getDeclaredField("queue");
		queueField.setAccessible(true);
		Object rawQueue = queueField.get(scanQueue);
		BlockingQueue<?> queue = (BlockingQueue<?>) rawQueue;
		assertFalse(queue.isEmpty());

		assertFalse(false);
	}
}
