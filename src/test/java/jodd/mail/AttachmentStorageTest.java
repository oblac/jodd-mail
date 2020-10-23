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

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jodd.io.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.util.List;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttachmentStorageTest {

	private static final String GREEN_MAIL_COM = "green@mail.com";
	private static final String GREEN = "green";
	private static final String PWD = "pwd";
	private static final String LOCALHOST = "localhost";
	private static final long TIMEOUT_IN_MS = 2000L;

	GreenMail greenMail;

	@BeforeEach
	void startGreenMailInstance() {
		final ServerSetup[] serverSetups = ServerSetupTest.ALL;
		for (ServerSetup setup : serverSetups){
			setup.setServerStartupTimeout(TIMEOUT_IN_MS);
		}
		greenMail = new GreenMail(serverSetups);
		greenMail.setUser(GREEN_MAIL_COM, GREEN, PWD);
		greenMail.start();
	}

	@AfterEach
	void stopGreenMailInstance() {
		if (greenMail != null) {
			greenMail.stop();
		}
	}

	@Test
	@EnabledOnOs(value = {OS.AIX, OS.LINUX, OS.MAC, OS.SOLARIS})
	void testAttachmentStorage() throws Exception {
		// storing files with its message-id as file name fails on windows because value of the message-id consists of brackets
		//		file names with brackets are not valid on window hosts
		// see https://tools.ietf.org/html/rfc5322#section-3.6.4

		final SmtpServer smtpServer = MailServer.create()
			.host(LOCALHOST)
			.port(3025)
			.buildSmtpMailServer();

		// prepare emails
		{
			final SendMailSession session = smtpServer.createSession();

			session.open();

			int count = 100;
			while (count-- > 0) {
				// create Email
				final Email sentEmail = Email.create()
					.subject("Mail : " + count)
					.from("Jodd", "jodd@use.me")
					.to(GREEN_MAIL_COM)
					.textMessage("Hello Hello " + count)
					.attachment(
						EmailAttachment
							.with()
							.name("a.jpg")
							.setContentIdFromNameIfMissing()
							.content(new byte[]{'X', 'Z', 'X'})
					).attachment(
						EmailAttachment
							.with()
							.name("")
							.content("ГИМНАСТИКА".getBytes())
					);
				session.sendMail(sentEmail);

				assertEquals(3, sentEmail.attachments().get(0).toByteArray().length);
				assertEquals(10*2, sentEmail.attachments().get(1).toByteArray().length);
			}

			session.close();
		}

		ReceivedEmail[] receivedEmails;

		// read attachments
		{
			final ImapServer imapServer = MailServer.create()
				.host(LOCALHOST)
				.port(3143)
				.auth(GREEN, PWD)
				.buildImapMailServer();

			final ReceiveMailSession session = imapServer.createSession();

			session.open();

			receivedEmails = session.receiveEmail();

			session.close();
		}

		for (final ReceivedEmail receivedEmail : receivedEmails) {
			final List<EmailAttachment<?>> attachmentList = receivedEmail.attachments();
			assertEquals(2, attachmentList.size());

			EmailAttachment att = attachmentList.get(0);
			assertEquals(3, att.toByteArray().length);

			att = attachmentList.get(1);
			assertEquals(20, att.toByteArray().length);
			assertEquals("ГИМНАСТИКА", new String(att.toByteArray()));
		}


		final File attFolder = FileUtil.createTempDirectory("jodd", "tt");

		// read and store attachments
		{
			final ImapServer imapServer = MailServer.create()
				.host(LOCALHOST)
				.port(3143)
				.auth(GREEN, PWD)
				.storeAttachmentsIn(attFolder)
				.buildImapMailServer();

			final ReceiveMailSession session = imapServer.createSession();

			session.open();

			receivedEmails = session.receiveEmail();

			session.close();
		}

		assertEquals(100, receivedEmails.length);
		final File[] allFiles = attFolder.listFiles();
		for (final File f : allFiles) {
			final byte[] bytes = FileUtil.readBytes(f);
			if (bytes.length == 3) {
				assertArrayEquals(new byte[]{'X', 'Z', 'X'}, bytes);
			}
			else if (bytes.length == 20) {
				assertArrayEquals("ГИМНАСТИКА".getBytes(), bytes);
			}
			else {
				fail();
			}
		}
		assertEquals(200, allFiles.length);

		for (final ReceivedEmail receivedEmail : receivedEmails) {
			final List<EmailAttachment<?>> attachmentList = receivedEmail.attachments();
			assertEquals(2, attachmentList.size());
			final EmailAttachment att = attachmentList.get(0);

			final byte[] bytes = att.toByteArray();
			if (bytes.length == 3) {
				assertArrayEquals(new byte[]{'X', 'Z', 'X'}, bytes);
			}
			else if (bytes.length == 20) {
				assertArrayEquals("ГИМНАСТИКА".getBytes(), bytes);
			}
			else {
				fail();
			}
		}

		FileUtil.deleteDir(attFolder);
	}

}
