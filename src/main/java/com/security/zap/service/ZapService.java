package com.security.zap.service;

import com.security.zap.entity.Endpoint;
import io.sentry.Sentry;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.net.ssl.SSLContext;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.*;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zaproxy.clientapi.core.ClientApi;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Log4j2
public class ZapService {

	private final ObjectMapper mapper = new ObjectMapper();
	private final CloseableHttpClient httpClient;
	private final ClientApi clientApi;

	@Value("${spring.zap.api}")
	private String ZAP_API;

	@Value("${spring.zap.key}")
	private String ZAP_KEY;

	public ZapService(ClientApi clientApi) throws Exception {
		SSLContext sslContext = SSLContextBuilder.create()
				.loadTrustMaterial(TrustAllStrategy.INSTANCE)
				.build();

		SSLConnectionSocketFactory sslSocketFactory =
				new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(Timeout.ofSeconds(30))
				.setResponseTimeout(Timeout.ofSeconds(60))
				.build();

		this.clientApi = clientApi;
		this.httpClient = HttpClients.custom()
				.setProxy(new HttpHost("localhost", 8090))
				.setConnectionManager(
						org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
								.setSSLSocketFactory(sslSocketFactory)
								.build())
				.setDefaultRequestConfig(config)
				.build();
	}

	/**
	 * Prepares a session for the ZAP client by creating a new session, setting specific scan options, configuring headers, and performing initial access to the target URL through ZAP.
	 * @param endpoint The Endpoint object containing the target URL and optional headers required for setting up the session and performing initial requests.
	 * @throws Exception If an error occurs while interacting with the ZAP client or during operations related to session setup.
	 */
	public void prepareSession(Endpoint endpoint) throws Exception {
		clientApi.core.newSession("zap_session_" + System.currentTimeMillis(), "true");
		clientApi.ascan.setOptionTargetParamsEnabledRPC(1);
		clientApi.ascan.setOptionTargetParamsInjectable(31);

		if (endpoint.getHeaders() != null) {
			setupZapHeaders(endpoint.getHeaders());
		}

		URI fullUri = buildUri(endpoint);
		clientApi.core.accessUrl(fullUri.toString(), "true");
		sendRequestThroughZap(endpoint, fullUri);

		Thread.sleep(2000);
	}

	/**
	 * Configures the required headers by transforming and appending them to the existing rules.
	 * @param headers a map of header names and their corresponding values, where each key-value pair represents a header to be processed and added to the rule set.
	 */
	private void setupZapHeaders(Map<String, Object> headers) {
		headers.forEach((k, v) -> upsertRule("rule-" + k, k, cleanValue(v)));
		upsertRule("force-json", "Content-Type", "application/json");
	}

	/**
	 * Adds or updates a rule in the replacer configuration. First, any existing rule with the given name is removed.
	 * @param ruleName The name of the rule to be added or updated.
	 * @param matchString The string pattern to be matched in the replacer rule.
	 * @param replacement The string to replace the matched pattern in the replacer rule.
	 */
	private void upsertRule(String ruleName, String matchString, String replacement) {
		try {
			clientApi.replacer.removeRule(ruleName);
		} catch (Exception e) {
			log.debug("Rule '{}' not found, skipping removal: {}", ruleName, e.getMessage());
		}
		try {
			clientApi.replacer.addRule(ruleName, "true", "REQ_HEADER", "false", matchString, replacement, null, null);
		} catch (Exception e) {
			Sentry.captureException(e);
			log.error("Error adding replacer rule '{}': {}", ruleName, e.getMessage());
		}
	}

	/**
	 * Sends an HTTP request using the ZAP framework based on the provided endpoint configuration and URI.
	 * @param endpoint The endpoint configuration that contains details like HTTP method, headers, and request body.
	 * @param uri The target URI to which the request will be sent.
	 * @throws IOException If an I/O error occurs while sending the request.
	 */
	private void sendRequestThroughZap(Endpoint endpoint, URI uri) throws IOException {
		HttpUriRequestBase request = new HttpUriRequestBase(endpoint.getHttpMethod(), uri);

		if (endpoint.getHeaders() != null) {
			endpoint.getHeaders().forEach((k, v) -> request.addHeader(k, cleanValue(v)));
		}

		if (endpoint.getRequestBody() != null && hasBody(endpoint.getHttpMethod())) {
			String jsonBody = mapper.writeValueAsString(endpoint.getRequestBody());
			request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
		}

		httpClient.execute(request, response -> null);
	}

	/**
	 * Constructs a URI by using the provided endpoint's base URL and query parameters.
	 * @param endpoint the endpoint containing the base URL and query parameters
	 * @return the constructed URI based on the endpoint's URL and query parameters
	 * @throws Exception if an error occurs while building the URI
	 */
	private URI buildUri(Endpoint endpoint) throws Exception {
		URIBuilder builder = new URIBuilder(endpoint.getUrl());
		if (endpoint.getQueryParams() != null) {
			endpoint.getQueryParams().forEach((k, v) -> builder.addParameter(k, cleanValue(v)));
		}
		return builder.build();
	}

	/**
	 * Cleans the provided value by removing leading and trailing double quotes if present. If the value is null, an empty string is returned.
	 * @param value the object to be cleaned, which may be null
	 * @return the cleaned string representation of the value without leading or trailing double quotes, or an empty string if the input value is null
	 */
	private String cleanValue(Object value) {
		if (value == null) {
			return "";
		}
		return String.valueOf(value).replaceAll("^\"|\"$", "");
	}

	/**
	 * Determines if the HTTP method typically includes a request body.
	 * @param method the HTTP method to evaluate
	 * @return true if the specified HTTP method typically includes a request body, false otherwise.
	 */
	private boolean hasBody(String method) {
		return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
	}

	/**
	 * Generates and retrieves an HTML security report from the ZAP API.
	 * @return A string containing the formatted HTML security report.
	 * @throws IOException If an I/O error occurs while fetching the report.
	 */
	public String getHtmlReport() throws IOException {
		String reportUrl = ZAP_API + "/OTHER/core/other/htmlreport/?apikey=" + ZAP_KEY;
		String rawHtml = callZap(reportUrl);

		Document doc = Jsoup.parse(rawHtml);
		doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
		doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
		return doc.html();
	}

	/**
	 * Initiates a scan on the provided URL using the ZAP API.
	 * @param encodedUrl the URL to be scanned, encoded as a string
	 * @return the scan ID as a string if the scan is successfully initiated
	 * @throws IOException if there is an error in the API response or communication issue
	 */
	public String startScan(String encodedUrl) throws IOException {
		String response = callZap(ZAP_API + "/JSON/ascan/action/scan/?apikey=" + ZAP_KEY + "&url=" + encodedUrl);
		JsonNode node = mapper.readTree(response);
		if (node.has("code")) {
			throw new IOException("ZAP Error: " + node.get("message").asString());
		}
		return node.get("scan").asString();
	}

	/**
	 * Sends an HTTP GET request to the specified URL and retrieves the response as a string.
	 * @param url the URL to send the GET request to
	 * @return the response body as a string
	 * @throws IOException if an I/O exception occurs during the request
	 */
	private String callZap(String url) throws IOException {
		return httpClient.execute(
				new org.apache.hc.client5.http.classic.methods.HttpGet(url),
				r -> new String(r.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8));
	}

	/**
	 * Retrieves the progress status of an active scan based on the provided scan ID.
	 * @param scanId the unique identifier of the scan whose progress is to be retrieved
	 * @return the scan progress as an integer percentage
	 * @throws IOException if an I/O error occurs during the API request
	 */
	public int getScanProgress(String scanId) throws IOException {
		String resp = callZap(ZAP_API + "/JSON/ascan/view/status/?scanId=" + scanId + "&apikey=" + ZAP_KEY);
		return mapper.readTree(resp).get("status").asInt();
	}

	/**
	 * Checks whether the ZAP instance is currently running by attempting to call the ZAP API and verifying the presence of a version key in the response.
	 * @return true if ZAP is running and the API responds with a valid version key; false otherwise
	 */
	public boolean isZapRunning() {
		try {
			String response = callZap(ZAP_API + "/JSON/core/view/version/?apikey=" + ZAP_KEY);
			if (response != null) {
				JsonNode jsonNode = mapper.readTree(response);
				return jsonNode.has("version");
			}
			return false;
		} catch (Exception e) {
			Sentry.captureException(e);
			return false;
		}
	}
}
