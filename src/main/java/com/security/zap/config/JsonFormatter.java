package com.security.zap.config;

import io.sentry.Sentry;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import tools.jackson.databind.ObjectMapper;

public record JsonFormatter(ObjectMapper objectMapper) implements FormatMapper {

	@Override
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions options) {
		try {
			return objectMapper.readValue(charSequence.toString(), objectMapper.constructType(javaType.getJavaType()));
		} catch (Exception e) {
			Sentry.captureException(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> String toString(T value, JavaType<T> javaType, WrapperOptions options) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			Sentry.captureException(e);
			throw new RuntimeException(e);
		}
	}
}
