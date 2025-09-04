package com.security.zap.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zaproxy.clientapi.core.ClientApi;

@Configuration
public class ZapConfig {

	@Bean
	public ClientApi clientApi(@Value("${spring.zap.api}") String zapApi, @Value("${spring.zap.key}") String apiKey)
			throws Exception {

		URI uri = new URI(zapApi);
		String host = uri.getHost();
		int port = uri.getPort();

		return new ClientApi(host, port, apiKey);
	}
}
