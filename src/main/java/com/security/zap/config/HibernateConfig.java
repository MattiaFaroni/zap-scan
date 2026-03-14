package com.security.zap.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class HibernateConfig {

	@Bean
	public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(ObjectMapper objectMapper) {
		return (properties) -> properties.put(AvailableSettings.JSON_FORMAT_MAPPER, new JsonFormatter(objectMapper));
	}
}
