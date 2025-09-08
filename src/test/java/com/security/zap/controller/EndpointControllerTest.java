package com.security.zap.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.zap.entity.Endpoint;
import com.security.zap.repository.EndpointRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class EndpointControllerTest {

	private MockMvc mockMvc;

	@Mock
	private EndpointRepository endpointRepository;

	@InjectMocks
	private EndpointController endpointController;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private Endpoint endpoint1;
	private Endpoint endpoint2;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(endpointController).build();

		endpoint1 = new Endpoint();
		endpoint1.setId(1);
		endpoint1.setName("Endpoint 1");
		endpoint1.setUrl("/api/endpoint1");
		endpoint1.setHttpMethod("GET");
		endpoint1.setQueryParams(Map.of("param1", "value1"));
		endpoint1.setHeaders(Map.of("header1", "value1"));
		endpoint1.setRequestBody(Map.of("key1", "value1"));

		endpoint2 = new Endpoint();
		endpoint2.setId(2);
		endpoint2.setName("Endpoint 2");
		endpoint2.setUrl("/api/endpoint2");
		endpoint2.setHttpMethod("POST");
		endpoint2.setQueryParams(Map.of("param2", "value2"));
		endpoint2.setHeaders(Map.of("header2", "value2"));
		endpoint2.setRequestBody(Map.of("key2", "value2"));
	}

	@Test
	void testGetAllEndpoints() throws Exception {
		List<Endpoint> endpoints = Arrays.asList(endpoint1, endpoint2);
		when(endpointRepository.findAll()).thenReturn(endpoints);

		mockMvc.perform(get("/scan/endpoints"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(objectMapper.writeValueAsString(endpoints)));

		verify(endpointRepository, times(1)).findAll();
	}

	@Test
	void testGetEndpointByIdFound() throws Exception {
		when(endpointRepository.findById(1)).thenReturn(Optional.of(endpoint1));

		mockMvc.perform(get("/scan/endpoints").param("id", "1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(objectMapper.writeValueAsString(endpoint1)));

		verify(endpointRepository, times(1)).findById(1);
	}

	@Test
	void testGetEndpointByIdNotFound() throws Exception {
		when(endpointRepository.findById(3)).thenReturn(Optional.empty());

		mockMvc.perform(get("/scan/endpoints").param("id", "3")).andExpect(status().isNotFound());

		verify(endpointRepository, times(1)).findById(3);
	}
}
