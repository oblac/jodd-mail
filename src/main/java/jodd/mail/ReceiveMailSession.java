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
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import java.io.File;
import java.util.function.Consumer;

/**
 * Encapsulates {@link Email} receiving session. Prepares and receives {@link Email}s.
 * Some methods do not work on POP3 servers.
 */
public class ReceiveMailSession extends MailSession<Store> {

	/**
	 * Default folder.
	 */
	protected static final String DEFAULT_FOLDER = "INBOX";

	/**
	 * The current folder.
	 */
	Folder folder;
	String folderName;

	final File attachmentStorage;

	static {
		setupSystemMailProperties();
	}

	/**
	 * Creates new mail session.
	 *
	 * @param session {@link Session}.
	 * @param store   {@link Store}.
	 */
	public ReceiveMailSession(final Session session, final Store store, final File attachmentStorage) {
		super(session, store);
		this.attachmentStorage = attachmentStorage;
	}

	@Override
	public Store getService() {
		return (Store) service;
	}

	// ---------------------------------------------------------------- folders

	/**
	 * Returns array of all {@link Folder}s as {@code String}s. You can use these names in
	 * {@link #useFolder(String)} method.
	 *
	 * @return array of all {@link Folder}s as {@code String}s.
	 */
	public String[] getAllFolders() {
		final Folder[] folders;
		try {
			folders = getService().getDefaultFolder().list("*");
		} catch (final MessagingException msgexc) {
			throw new MailException("Failed to connect to folder", msgexc);
		}
		final String[] folderNames = new String[folders.length];

		for (int i = 0; i < folders.length; i++) {
			final Folder folder = folders[i];
			folderNames[i] = folder.getFullName();
		}
		return folderNames;
	}

	/**
	 * Opens new folder and closes previously opened folder.
	 *
	 * @param folderName Folder to open
	 */
	public void useFolder(final String folderName) {
		useFolder(folderName, Folder.READ_WRITE);
	}

	/**
	 * Opens new folder and closes previously opened folder with a specific mode.
	 *
	 * @param folderName Folder to open
	 * @param mode Mode to set
	 */
	public void useFolder(final String folderName, final int mode) {
		closeFolderIfOpened(folder);

		try {
			this.folderName = folderName;
			this.folder = getService().getFolder(folderName);
			try {
				folder.open(mode);
			} catch (final MailException ignore) {
				folder.open(Folder.READ_ONLY);
			}
		} catch (final MessagingException msgexc) {
			throw new MailException("Failed to connect to folder: " + folderName, msgexc);
		}
	}

	/**
	 * Just returns a folder, w/o opening.
	 */
	public Folder getFolder(final String folder) {
		try {
			return getService().getFolder(folder);
		} catch (final MessagingException e) {
			throw new MailException("Folder not found: " + folder, e);
		}
	}

	// ---------------------------------------------------------------- open

	/**
	 * Opens default folder: DEFAULT_FOLDER.
	 */
	public void useDefaultFolder() {
		closeFolderIfOpened(folder);
		useFolder(DEFAULT_FOLDER);
	}

	private void useAndOpenFolderIfNotSet() {
		if (folder == null) {
			if (folderName != null) {
				useFolder(folderName);
			}
			else {
				useDefaultFolder();
			}
		}
	}

	// ---------------------------------------------------------------- message count

	/**
	 * Returns number of messages.
	 *
	 * @return The number of messages.
	 */
	public int getMessageCount() {
		useAndOpenFolderIfNotSet();
		try {
			return folder.getMessageCount();
		} catch (final MessagingException msgexc) {
			throw new MailException(msgexc);
		}
	}

	/**
	 * Returns the number of new messages.
	 *
	 * @return The number of new message.
	 */
	public int getNewMessageCount() {
		useAndOpenFolderIfNotSet();
		try {
			return folder.getNewMessageCount();
		} catch (final MessagingException msgexc) {
			throw new MailException(msgexc);
		}
	}

	/**
	 * Returns the number of unread messages.
	 */
	public int getUnreadMessageCount() {
		useAndOpenFolderIfNotSet();
		try {
			return folder.getUnreadMessageCount();
		} catch (final MessagingException msgexc) {
			throw new MailException(msgexc);
		}
	}

	/**
	 * Returns the number of deleted messages.
	 *
	 * @return The number of deleted messages.
	 */
	public int getDeletedMessageCount() {
		useAndOpenFolderIfNotSet();
		try {
			return folder.getDeletedMessageCount();
		} catch (final MessagingException msgexc) {
			throw new MailException(msgexc);
		}
	}

	// ---------------------------------------------------------------- receive builder

	/**
	 * Defines the process of received email in a fluent way.
	 */
	public ReceiverBuilder receive() {
		return new ReceiverBuilder(this);
	}

	// ---------------------------------------------------------------- receive emails

	/**
	 * Receives all emails. Messages are not modified. However, servers
	 * may set SEEN flag anyway, so we force messages to remain
	 * unseen.
	 *
	 * @return array of {@link ReceivedEmail}s.
	 */
	public ReceivedEmail[] receiveEmail() {
		return receiveMessages(null, null, null, false, null).fetch();
	}

	/**
	 * Receives all emails that matches given {@link EmailFilter}.
	 * Messages are not modified. However, servers may set SEEN flag anyway,
	 * so we force messages to remain unseen.
	 *
	 * @param filter {@link EmailFilter}
	 * @return array of {@link ReceivedEmail}s.
	 */
	public ReceivedEmail[] receiveEmail(final EmailFilter filter) {
		return receiveMessages(filter, null, null, false, null).fetch();
	}

	/**
	 * Receives all emails and mark all messages as 'seen' (ie 'read').
	 *
	 * @return array of {@link ReceivedEmail}s.
	 * @see #receiveEmailAndMarkSeen(EmailFilter)
	 */
	public ReceivedEmail[] receiveEmailAndMarkSeen() {
		return receiveEmailAndMarkSeen(null);
	}

	/**
	 * Receives all emails that matches given {@link EmailFilter}
	 * and mark them as 'seen' (ie 'read').
	 *
	 * @param filter {@link EmailFilter}
	 * @return array of {@link ReceivedEmail}s.
	 */
	public ReceivedEmail[] receiveEmailAndMarkSeen(final EmailFilter filter) {
		final Flags flagsToSet = new Flags();
		flagsToSet.add(Flags.Flag.SEEN);
		return receiveMessages(filter, flagsToSet, null, false, null).fetch();
	}

	/**
	 * Receives all emails and mark all messages as 'seen' and 'deleted'.
	 *
	 * @return array of {@link ReceivedEmail}s.
	 */
	public ReceivedEmail[] receiveEmailAndDelete() {
		return receiveEmailAndDelete(null);
	}

	/**
	 * Receives all emails that matches given {@link EmailFilter} and
	 * mark all messages as 'seen' and 'deleted'.
	 *
	 * @param filter {@link EmailFilter}
	 * @return array of {@link ReceivedEmail}s.
	 */
	public ReceivedEmail[] receiveEmailAndDelete(final EmailFilter filter) {
		final Flags flags = new Flags();
		flags.add(Flags.Flag.SEEN);
		flags.add(Flags.Flag.DELETED);
		return receiveMessages(filter, flags, null, false, null).fetch();
	}

	public ReceivedEmail[] receiveEnvelopes() {
		return receiveEnvelopes(null);
	}

	public ReceivedEmail[] receiveEnvelopes(final EmailFilter filter) {
		return receiveMessages(filter, null, null, true, null).fetch();
	}

	/**
	 * The main email receiving method.
	 */
	ReceivedEmails receiveMessages(
			final EmailFilter filter,
			final Flags flagsToSet,
			final Flags flagsToUnset,
			final boolean envelope,
			final Consumer<Message[]> processedMessageConsumer) {
		useAndOpenFolderIfNotSet();

		final Message[] messages;

		try {
			if (filter == null) {
				messages = folder.getMessages();
			} else {
				messages = folder.search(filter.getSearchTerm());
			}

			return new ReceivedEmails(this, messages, flagsToSet, flagsToUnset, envelope, processedMessageConsumer);
		} catch (final MessagingException msgexc) {
			throw new MailException("Failed to fetch messages", msgexc);
		}
	}


	// ---------------------------------------------------------------- update

	/**
	 * Updates the email flags on the server.
	 */
	public void updateEmailFlags(final ReceivedEmail receivedEmail) {
		useAndOpenFolderIfNotSet();
		try {
			folder.setFlags(new int[] {receivedEmail.messageNumber()}, receivedEmail.flags(),true);
		} catch (final MessagingException mex) {
			throw new MailException("Failed to fetch messages", mex);
		}
	}

	// ---------------------------------------------------------------- close

	/**
	 * Closes folder if opened and expunge deleted messages.
	 */
	protected static void closeFolderIfOpened(final Folder folder) {
		if (folder != null && folder.isOpen()) {
			try {
				folder.close(true);
			} catch (final MessagingException ignore) {
			}
		}
	}

	@Override
	public void close() {
		closeFolderIfOpened(folder);
		folder = null;
		folderName = null;
		super.close();
	}

}
