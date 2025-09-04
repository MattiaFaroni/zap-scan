package com.security.zap.service;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class MailService {

	private final JavaMailSender mailSender;
	private final String from;
	private final List<String> to;
	private final String subject;

	public MailService(
			JavaMailSender mailSender,
			@Value("${spring.mail.from}") String from,
			@Value("#{'${spring.mail.to}'.split(',')}") List<String> to,
			@Value("${spring.mail.subject}") String subject) {
		this.mailSender = mailSender;
		this.from = from;
		this.to = to;
		this.subject = subject;
	}

	/**
	 * Sends PDF reports as email attachments to the configured recipients.
	 * @param attachments a list of {@link ReportAttachment} objects containing the
	 *                    filenames and content of the PDF reports to be sent.
	 * @throws Exception if an error occurs during email preparation or sending,
	 *                   including issues with loading the email template.
	 */
	public void sendPdfReports(List<ReportAttachment> attachments) throws Exception {

		String htmlContent;
		try {
			htmlContent = loadReportTemplate();
		} catch (Exception e) {
			log.error("Report template loading error, email will NOT be sent", e);
			throw e;
		}

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

		helper.setFrom(from);
		helper.setTo(to.toArray(new String[0]));
		helper.setSubject(subject);
		helper.setText(htmlContent, true);

		for (ReportAttachment attachment : attachments) {
			helper.addAttachment(attachment.filename(), new ByteArrayResource(attachment.content()));
		}

		mailSender.send(message);
	}

	/**
	 * Loads the HTML template for the report from the application's classpath.
	 * @return the content of the "report-template.html" file as a string.
	 * @throws IOException if an error occurs while accessing or reading the template file.
	 */
	private static String loadReportTemplate() throws IOException {
		ClassPathResource resource = new ClassPathResource("report-template.html");
		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public record ReportAttachment(String filename, byte[] content) {}
}
