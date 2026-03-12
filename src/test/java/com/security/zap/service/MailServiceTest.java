package com.security.zap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	private MailService mailService;

	private static final String FROM = "from@example.com";
	private static final String TO_1 = "to1@example.com";
	private static final String TO_2 = "to2@example.com";
	private static final String SUBJECT = "Report Subject";

	@BeforeEach
	void setUp() {
		mailService = new MailService(mailSender, FROM, List.of(TO_1, TO_2), SUBJECT);
	}

	@Test
	void testSendPdfReports_SingleAttachment() throws Exception {
		when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());

		MailService.ReportAttachment attachment =
				new MailService.ReportAttachment("report1.pdf", "PDF Content".getBytes(StandardCharsets.UTF_8));

		mailService.sendPdfReports(List.of(attachment));

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());

		MimeMessage sent = captor.getValue();
		assertNotNull(sent.getFrom());
		assertEquals(FROM, sent.getFrom()[0].toString());
		assertEquals(2, sent.getAllRecipients().length);
		assertEquals(TO_1, sent.getAllRecipients()[0].toString());
		assertEquals(TO_2, sent.getAllRecipients()[1].toString());
		assertEquals(SUBJECT, sent.getSubject());
	}

	@Test
	void testSendPdfReports_MultipleAttachments() throws Exception {
		when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());

		mailService.sendPdfReports(List.of(
				new MailService.ReportAttachment("report1.pdf", "Content1".getBytes(StandardCharsets.UTF_8)),
				new MailService.ReportAttachment("report2.pdf", "Content2".getBytes(StandardCharsets.UTF_8))));

		verify(mailSender, times(1)).send(any(MimeMessage.class));
	}

	@Test
	void testSendPdfReports_EmptyList_DoesNotSend() throws Exception {
		mailService.sendPdfReports(List.of());
		verify(mailSender, never()).send(any(MimeMessage.class));
		verify(mailSender, never()).createMimeMessage();
	}

	@Test
	void testSendPdfReports_MailSenderThrows_PropagatesException() {
		when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
		doThrow(new org.springframework.mail.MailSendException("SMTP error"))
				.when(mailSender)
				.send(any(MimeMessage.class));

		assertThrows(
				Exception.class,
				() -> mailService.sendPdfReports(List.of(
						new MailService.ReportAttachment("report1.pdf", "Content".getBytes(StandardCharsets.UTF_8)))));
	}
}
