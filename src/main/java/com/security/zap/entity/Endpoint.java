package com.security.zap.entity;

import jakarta.persistence.*;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "endpoints")
public class Endpoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
	private String name;

	@Column(name = "url", nullable = false, length = Integer.MAX_VALUE)
	private String url;

	@Column(name = "http_method", nullable = false, length = 10)
	private String httpMethod;

	@Column(name = "query_params")
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> queryParams;

	@Column(name = "headers")
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> headers;

	@Column(name = "request_body")
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> requestBody;
}
