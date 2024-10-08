// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jodd.mail;

import jakarta.mail.Flags;
import jodd.net.MimeTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailUtilTest {

	protected String testDataRoot;

	@BeforeEach
	void setUp() {
		if (testDataRoot != null) {
			return;
		}
		final URL data = EmailUtilTest.class.getResource("test");
		testDataRoot = data.getFile();
	}

	@Test
	void testExtractContentType() {
		String contentType = "multipart/mixed;";
		assertEquals("multipart/mixed", EmailUtil.extractMimeType(contentType));
		assertNull(EmailUtil.extractEncoding(contentType));

		contentType = "multipart/mixed; boundary=-----";
		assertEquals("multipart/mixed", EmailUtil.extractMimeType(contentType));
		assertNull(EmailUtil.extractEncoding(contentType));

		contentType = "text/html;\n\tcharset=\"us-ascii\"";
		assertEquals(MimeTypes.MIME_TEXT_HTML, EmailUtil.extractMimeType(contentType));
		assertEquals(StandardCharsets.US_ASCII.name().toLowerCase(), EmailUtil.extractEncoding(contentType));

		contentType = "TEXT/PLAIN; charset=US-ASCII; name=example.eml";
		assertEquals(MimeTypes.MIME_TEXT_PLAIN.toUpperCase(), EmailUtil.extractMimeType(contentType));
		assertEquals(StandardCharsets.US_ASCII.name(), EmailUtil.extractEncoding(contentType));
	}

	@Test
	void testIsEmptyFlags() {
		Flags flags = new Flags();
		flags.add(Flags.Flag.DELETED);
		assertTrue(!EmailUtil.isEmptyFlags(flags));

		flags = new Flags();
		flags.add("userFlag");
		assertTrue(!EmailUtil.isEmptyFlags(flags));

		flags = new Flags();
		assertTrue(EmailUtil.isEmptyFlags(flags));

		flags = null;
		assertTrue(EmailUtil.isEmptyFlags(flags));
	}

	@Test
	void testSanitizeFileName() {
		assertEquals("file.txt", EmailUtil.sanitizeFileName("file.txt"));
		assertEquals("_6d0455f09ad249c897c0aa28a7ee3579_domain_", EmailUtil.sanitizeFileName("<6d0455f09ad249c897c0aa28a7ee3579@domain>"));
	}

}
