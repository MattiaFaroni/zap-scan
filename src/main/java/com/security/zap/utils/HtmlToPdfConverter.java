package com.security.zap.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;

public class HtmlToPdfConverter {

	/**
	 * Converts the given HTML content into a PDF document and returns the PDF as a byte array.
	 * @param html the HTML content to be converted into a PDF document
	 * @return a byte array containing the PDF representation of the given HTML content
	 * @throws Exception if an error occurs during the PDF conversion process
	 */
	public static byte[] convert(String html) throws Exception {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(html, null);
			builder.toStream(outputStream);
			builder.run();
			return outputStream.toByteArray();
		}
	}
}
