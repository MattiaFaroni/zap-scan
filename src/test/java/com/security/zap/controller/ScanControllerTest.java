package com.security.zap.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.security.zap.job.ScanJob;
import com.security.zap.job.ScanQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ScanControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ScanQueue scanQueue;

	@InjectMocks
	private ScanController scanController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(scanController).build();
	}

	@Test
	void testStreamScanWithEndpointId() throws Exception {
		Integer endpointId = 1;

		mockMvc.perform(MockMvcRequestBuilders.get("/scan/stream")
						.param("endpoint_id", endpointId.toString())
						.accept(MediaType.TEXT_EVENT_STREAM))
				.andReturn();

		ArgumentCaptor<ScanJob> captor = ArgumentCaptor.forClass(ScanJob.class);
		verify(scanQueue, times(1)).enqueue(captor.capture());

		ScanJob job = captor.getValue();
		assertNotNull(job);
		assertEquals(endpointId, job.endpointId());
		assertNotNull(job.emitter());
	}

	@Test
	void testStreamScanWithoutEndpointId() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/scan/stream").accept(MediaType.TEXT_EVENT_STREAM))
				.andReturn();

		ArgumentCaptor<ScanJob> captor = ArgumentCaptor.forClass(ScanJob.class);
		verify(scanQueue, times(1)).enqueue(captor.capture());

		ScanJob job = captor.getValue();
		assertNotNull(job);
		assertNull(job.endpointId());
		assertNotNull(job.emitter());
	}
}
