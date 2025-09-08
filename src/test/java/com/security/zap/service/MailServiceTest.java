package com.security.zap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private MailService mailService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		JavaMailSenderImpl realSender = new JavaMailSenderImpl();
		MimeMessage mimeMessage = realSender.createMimeMessage();

		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		mailService = new MailService(
				mailSender, "from@example.com", List.of("to1@example.com", "to2@example.com"), "Report Subject");
	}

	@Test
	void testSendPdfReports() throws Exception {
		MailService.ReportAttachment attachment =
				new MailService.ReportAttachment("report1.pdf", "PDF Content".getBytes(StandardCharsets.UTF_8));

		mailService.sendPdfReports(List.of(attachment));

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());

		MimeMessage sentMessage = captor.getValue();

		assertNotNull(sentMessage.getFrom());
		assertEquals("from@example.com", sentMessage.getFrom()[0].toString());

		assertNotNull(sentMessage.getAllRecipients());
		assertEquals("to1@example.com", sentMessage.getAllRecipients()[0].toString());
		assertEquals("to2@example.com", sentMessage.getAllRecipients()[1].toString());

		assertEquals("Report Subject", sentMessage.getSubject());
	}
}
