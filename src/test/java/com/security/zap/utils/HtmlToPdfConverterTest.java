package com.security.zap.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HtmlToPdfConverterTest {

	private static final String VALID_HTML = "<html><body><h1>Test PDF</h1><p>This is a paragraph.</p></body></html>";
	private static final String PDF_MAGIC = "%PDF-";

	@Test
	void testConvert_ValidHtml_ReturnsPdf() throws Exception {
		byte[] pdfBytes = HtmlToPdfConverter.convert(VALID_HTML);

		assertNotNull(pdfBytes, "The PDF must not be null");
		assertTrue(pdfBytes.length > 0, "The PDF must not be empty");
		assertEquals(PDF_MAGIC, new String(pdfBytes, 0, 5), "Output must start with PDF magic bytes");
	}

	@Test
	void testConvert_MinimalHtml_ReturnsPdf() throws Exception {
		byte[] pdfBytes = HtmlToPdfConverter.convert("<html></html>");
		assertNotNull(pdfBytes);
		assertEquals(PDF_MAGIC, new String(pdfBytes, 0, 5));
	}

	@Test
	void testConvert_WithSpecialCharacters_DoesNotThrow() {
		String htmlWithSpecialChars =
				"<html><body><p>Test &amp; &lt;verify&gt; \"quoted\" 'apostrophe'</p></body></html>";
		assertDoesNotThrow(() -> HtmlToPdfConverter.convert(htmlWithSpecialChars));
	}

	@Test
	void testConvert_NullHtml_ThrowsException() {
		assertThrows(Exception.class, () -> HtmlToPdfConverter.convert(null));
	}
}
