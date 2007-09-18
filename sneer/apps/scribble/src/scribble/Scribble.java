package scribble;

import static wheel.i18n.Language.translate;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scribble.gui.ScribbleFrame;
import sneer.kernel.appmanager.AppConfig;
import sneer.kernel.business.contacts.ContactId;
import sneer.kernel.communication.Channel;
import sneer.kernel.communication.Packet;
import sneer.kernel.gui.contacts.ContactAction;
import sneer.kernel.pointofview.Contact;
import wheel.io.ui.User;
import wheel.lang.Omnivore;
import wheel.reactive.Signal;
import wheel.reactive.Source;
import wheel.reactive.impl.SourceImpl;
import wheel.reactive.lists.ListSignal;

public class Scribble {

	private static final String OPEN_REQUEST = "Request";
	private static final String OPEN_REQUEST_ACCEPTED = "Accepted";
	private static final String OPEN_REQUEST_DENIED = "Denied";
	private static final String CLOSE_REQUEST = "Close";

	private final Map<ContactId, ScribbleFrame> _framesByContactId = new HashMap<ContactId, ScribbleFrame>();
	private final Map<ContactId, SourceImpl<ScribblePacket>> _inputsByContactId = new HashMap<ContactId, SourceImpl<ScribblePacket>>();

	private final User _user;
	private final ListSignal<Contact> _contacts;
	private final Channel _channel;
	private Omnivore<Packet> _drawPacketReceiver;

	public Scribble(AppConfig config) {
		_user = config._user;
		_channel = config._channel;
		_contacts = config._contacts;
		_drawPacketReceiver = drawPacketReceiver();
		_channel.input().addReceiver(_drawPacketReceiver);
	}

	public List<ContactAction> contactActions() {
		return Collections.singletonList( (ContactAction)new ContactAction() {

			@Override
			public void actUpon(Contact contact) {
				actUponContact(contact);
			}

			@Override
			public String caption() {
				return translate("Scribble");
			}

		});
	}

	private Omnivore<Packet> drawPacketReceiver() {
		return new Omnivore<Packet>() {
			public void consume(Packet packet) {
				if (OPEN_REQUEST.equals(packet._contents)) {
					userWantsToOpen(packet._contactId);
					return;
				}

				if (CLOSE_REQUEST.equals(packet._contents)) {
					close(packet._contactId);
					return;
				}

				if (OPEN_REQUEST_ACCEPTED.equals(packet._contents)) {
					open(packet._contactId);
					return;
				}

				if (OPEN_REQUEST_DENIED.equals(packet._contents)) {
					_user.modelessAcknowledge(translate("Information"), translate("You request to %1$s was not accepted. :(","scribble"));
					return;
				}
				
				Source<ScribblePacket> input = getInputFor(packet._contactId);
				if (input == null)
					return;
				input.setter().consume((ScribblePacket) packet._contents);
			}
		};

	}

	private void userWantsToOpen(final ContactId contactId) {
		String nick = findContact(contactId).nick().currentValue();
		String prompt = translate("%1$s is inviting you to scribble. :)\n\nWill you accept?", nick);
		_user.confirmWithTimeout(prompt, 15, new Omnivore<Boolean>() { public void consume(Boolean accepted) {
			if (accepted) {
				open(contactId);
				sendTo(contactId, OPEN_REQUEST_ACCEPTED);
			} else
				sendTo(contactId, OPEN_REQUEST_DENIED);
		}});
	}

	private void close(ContactId contactId) {
		_inputsByContactId.remove(contactId);
		ScribbleFrame frame = _framesByContactId.remove(contactId);
		if (frame != null)
			frame.close();
	}

	private void open(ContactId contactId) {
		createInputFor(contactId);
		createFrameFor(contactId);
		findContact(contactId).party().isOnline().addReceiver(offlineCloser(contactId));
	}

	private Omnivore<Boolean> offlineCloser(final ContactId contactId) {
		return new Omnivore<Boolean>() {
			@Override
			public void consume(Boolean isOnline) {
				if (!isOnline)
					close(contactId);
			}
		};
	}

	private void actUponContact(Contact contact) {
		if (getInputFor(contact.id()) != null)
			return;
		sendTo(contact.id(), OPEN_REQUEST);
	}

	private void sendTo(ContactId contactId, Object contents) {
		_channel.output().consume(new Packet(contactId, contents));
	}

	private void createFrameFor(ContactId contactId) {
		ScribbleFrame frame = new ScribbleFrame(_user,findContact(contactId).nick(), inputFrom(contactId), outputTo(contactId));
		frame.addWindowListener(closingListener(contactId));
		_framesByContactId.put(contactId, frame);
	}

	private WindowListener closingListener(final ContactId contactId) {
		return new WindowAdapter() { @Override
			public void windowClosing(WindowEvent ignored) {
				close(contactId);
				sendTo(contactId, CLOSE_REQUEST);
			}
		};
	}

	private Contact findContact(ContactId id) {
		for (Contact candidate : _contacts)
			if (candidate.id().equals(id))
				return candidate;
		return null;
	}

	private Signal<ScribblePacket> inputFrom(ContactId contactId) {
		return getInputFor(contactId).output();
	}

	private Source<ScribblePacket> getInputFor(ContactId contactId) {
		return _inputsByContactId.get(contactId);
	}

	private void createInputFor(ContactId contactId) {
		SourceImpl<ScribblePacket> input = new SourceImpl<ScribblePacket>(null);
		_inputsByContactId.put(contactId, input);
	}

	private Omnivore<ScribblePacket> outputTo(final ContactId contactId) {
		return new Omnivore<ScribblePacket>() {
			public void consume(ScribblePacket audioPacket) {
				_channel.output().consume(new Packet(contactId, audioPacket));
			}
		};
	}

}
