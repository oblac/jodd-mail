package jodd.mail;

import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import java.util.function.Consumer;

public class ReceivedEmails {
	private final Message[] messages;
	private final boolean envelope;
	private final ReceiveMailSession session;
	private final Flags flagsToSet;
	private final Flags flagsToUnset;
	private final Consumer<Message[]> processedMessageConsumer;

	public ReceivedEmails(
			final ReceiveMailSession session,
			final Message[] messages,
			final Flags flagsToSet,
			final Flags flagsToUnset,
			final boolean envelope,
			final Consumer<Message[]> processedMessageConsumer
	) {
		this.session = session;
		this.messages = messages;
		this.flagsToSet = flagsToSet;
		this.flagsToUnset = flagsToUnset;
		this.envelope = envelope;
		this.processedMessageConsumer = processedMessageConsumer;
	}

	public ReceivedEmail[] fetch() {
		try {
			return _fetch();
		}
		catch (final MessagingException ex) {
			throw new MailException("Failed to process fetched messages", ex);
		}
	}

	@SuppressWarnings("t")
	private ReceivedEmail[] _fetch() throws MessagingException {
		if (messages.length == 0) {
			return ReceivedEmail.EMPTY_ARRAY;
		}

		final boolean isReadOnly = session.folder.getMode() == Folder.READ_ONLY;

		if (envelope) {
			final FetchProfile fetchProfile = new FetchProfile();

			fetchProfile.add(FetchProfile.Item.ENVELOPE);
			fetchProfile.add(FetchProfile.Item.FLAGS);

			session.folder.fetch(messages, fetchProfile);
		}

		final ReceivedEmail[] emails = new ReceivedEmail[messages.length];

		for (int i = 0; i < messages.length; i++) {
			final Message msg = messages[i];

			// we need to parse message BEFORE flags are set!
			emails[i] = new ReceivedEmail(msg, envelope, session.attachmentStorage);

			if (!EmailUtil.isEmptyFlags(flagsToSet)) {
				emails[i].flags(flagsToSet);
				if (!isReadOnly) {
					msg.setFlags(flagsToSet, true);
				}
			}

			if (!EmailUtil.isEmptyFlags(flagsToUnset)) {
				emails[i].flags().remove(flagsToUnset);
				if (!isReadOnly) {
					msg.setFlags(flagsToUnset, false);
				}
			}

			if (EmailUtil.isEmptyFlags(flagsToSet) && !emails[i].isSeen()) {
				if (!isReadOnly) {
					msg.setFlag(Flags.Flag.SEEN, false);
				}
			}
		}

		if (processedMessageConsumer != null) {
			processedMessageConsumer.accept(messages);
		}

		// if messages were marked to be deleted, we need to expunge the folder
		if (!EmailUtil.isEmptyFlags(flagsToSet) && !isReadOnly) {
			if (flagsToSet.contains(Flags.Flag.DELETED)) {
				session.folder.expunge();
			}
		}

		return emails;
	}

	public static ReceivedEmails empty() {
		return new ReceivedEmails(null, new Message[0], null, null, false, null);
	}
}
