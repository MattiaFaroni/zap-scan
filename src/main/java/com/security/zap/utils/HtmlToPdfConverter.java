package com.security.zap.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;

public class HtmlToPdfConverter {

	/**
	 * Converts an HTML string into a PDF represented as a byte array.
	 * @param html the HTML content to be converted into a PDF; must not be null.
	 * @return a byte array containing the binary content of the generated PDF.
	 * @throws Exception if an error occurs during the conversion process.
	 */
	public static byte[] convert(String html) throws Exception {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.usePdfVersion(1.7f);
			builder.usePdfUaAccessibility(false);
			builder.withHtmlContent(html, null);
			builder.toStream(outputStream);
			builder.run();
			return outputStream.toByteArray();
		}
	}
}
