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
import jakarta.mail.MessagingException;

import java.util.function.Consumer;

/**
 * Fluent builder
 */
public class ReceiverBuilder {

	private final ReceiveMailSession session;
	private EmailFilter filter;
	private final Flags flagsToSet = new Flags();
	private final Flags flagsToUnset = new Flags();
	private boolean envelopeOnly;
	private String targetFolder;
	private String fromFolder;

	public ReceiverBuilder(final ReceiveMailSession session) {
		this.session = session;
	}

	/**
	 * Define applied filters.
	 */
	public ReceiverBuilder filter(final EmailFilter emailFilter) {
		this.filter = emailFilter;
		return this;
	}

	/**
	 * Marks messages as seen after receiving them.
	 */
	public ReceiverBuilder markSeen() {
		this.flagsToSet.add(Flags.Flag.SEEN);
		return this;
	}

	/**
	 * Marks message with given flag.
	 */
	public ReceiverBuilder mark(final Flags.Flag flagToSet) {
		this.flagsToSet.add(flagToSet);
		return this;
	}

	/**
	 * Unmarks a message with given flag.
	 */
	public ReceiverBuilder unmark(final Flags.Flag flagToUnset) {
		this.flagsToUnset.add(flagToUnset);
		return this;
	}

	/**
	 * Deletes messages upon receiving.
	 */
	public ReceiverBuilder markDeleted() {
		this.flagsToSet.add(Flags.Flag.DELETED);
		return this;
	}

	/**
	 * Sets the working folder.
\	 */
	public ReceiverBuilder fromFolder(final String fromFolder) {
		this.fromFolder = fromFolder;
		return this;
	}

	/**
	 * Defines target folder where message will be moved.
	 */
	public ReceiverBuilder moveToFolder(final String targetFolder) {
		this.markDeleted();
		this.targetFolder = targetFolder;
		return this;
	}

	/**
	 * Receives only envelopes.
	 */
	public ReceiverBuilder envelopeOnly() {
		this.envelopeOnly = true;
		return this;
	}

	/**
	 * Receives the emails as specified by the builder.
	 */
	public ReceivedEmail[] get() {
		return with(ReceiverRunner.EMPTY_CONSUMER).fetch();
	}

	/**
	 * Wraps the received emails, so they not get processed until user really needs them.
	 */
	public ReceiverRunner with(final Consumer<ReceivedEmails> consumer) {
		if (fromFolder != null) {
			session.useFolder(fromFolder);
		}

		final ReceivedEmails receivedEmails = session.receiveMessages(filter, flagsToSet, flagsToUnset, envelopeOnly, messages -> {
			if (targetFolder != null) {
				try {
					session.folder.copyMessages(messages, session.getFolder(targetFolder));
				} catch (final MessagingException e) {
					throw new MailException("Copying messages failed");
				}
			}
		});
		return new ReceiverRunner(receivedEmails, consumer);
	}

	public static class ReceiverRunner {
		private final ReceivedEmails receivedEmails;
		private final Consumer<ReceivedEmails> consumer;

		private final static Consumer<ReceivedEmails> EMPTY_CONSUMER = receivedEmails -> {};

		private ReceiverRunner(final ReceivedEmails receivedEmails, final Consumer<ReceivedEmails> consumer) {
			this.receivedEmails = receivedEmails;
			this.consumer = consumer;
		}

		/**
		 * Executes the consumer on the received emails.
		 * At this point, the emails are not processed yet.
		 */
		final ReceiverRunner run() {
			consumer.accept(receivedEmails);
			return this;
		}

		/**
		 * Fetches the emails.
		 * After this point, the emails are processed.
		 */
		final ReceivedEmail[] fetch() {
			return receivedEmails.fetch();
		}
	}

}
