package com.security.zap.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HtmlToPdfConverterTest {

	@Test
	void testConvertHtml() throws Exception {
		String html = "<html><body><h1>Test PDF</h1><p>This is a paragraph.</p></body></html>";

		byte[] pdfBytes = HtmlToPdfConverter.convert(html);

		assertNotNull(pdfBytes, "The PDF must not be null");
		assertTrue(pdfBytes.length > 0, "The PDF must not be blank");

		String pdfHeader = new String(pdfBytes, 0, 5);
		assertEquals("%PDF-", pdfHeader);
	}
}
