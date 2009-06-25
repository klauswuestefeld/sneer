package sneer.bricks.network.computers.sockets.connections.receiver.tests;

import static sneer.foundation.environments.Environments.my;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.Ignore;
import org.junit.Test;

import sneer.bricks.hardware.cpu.threads.Threads;
import sneer.bricks.network.computers.sockets.accepter.SocketAccepter;
import sneer.bricks.network.computers.sockets.connections.ConnectionManager;
import sneer.bricks.network.computers.sockets.connections.receiver.SocketReceiver;
import sneer.bricks.network.computers.sockets.protocol.ProtocolTokens;
import sneer.bricks.network.social.ContactManager;
import sneer.bricks.pulp.events.EventNotifier;
import sneer.bricks.pulp.events.EventNotifiers;
import sneer.bricks.pulp.network.ByteArraySocket;
import sneer.foundation.brickness.testsupport.Bind;
import sneer.foundation.brickness.testsupport.BrickTest;

public class SocketReceiverTest extends BrickTest {

	@Bind private final SocketAccepter _socketAccepterMock = mock(SocketAccepter.class);
	private final ByteArraySocket _acceptedSocket = mock(ByteArraySocket.class);
	
	@Bind private final ConnectionManager connectionManagerMock = mock(ConnectionManager.class);

	@SuppressWarnings("unused")
	private SocketReceiver _subject;
	private final EventNotifier<ByteArraySocket> _acceptedSocketNotifier = my(EventNotifiers.class).create();
	private final ContactManager contactManager = my(ContactManager.class);
	
	@Ignore
	@Test
	public void reception() throws Exception {
		checking(new Expectations() {{
			Sequence sequence = newSequence("main");
			exactly(1).of(_socketAccepterMock).lastAcceptedSocket();
				will(returnValue(_acceptedSocketNotifier.output()));

			exactly(1).of(_acceptedSocket).read(); inSequence(sequence);
				will(returnValue(ProtocolTokens.SNEER_WIRE_PROTOCOL_1));
			exactly(1).of(_acceptedSocket).read(); inSequence(sequence);
				will(returnValue("Neide".getBytes("UTF-8")));
			exactly(1).of(_acceptedSocket).write(ProtocolTokens.OK); inSequence(sequence);
			
			exactly(1).of(connectionManagerMock).manageIncomingSocket(null, _acceptedSocket);
				//Capturar parametro e fazer assertSame(my(ContactManager.class).contactGiven("Neide"), parametro);
		}});

		_subject = my(SocketReceiver.class);

		_acceptedSocketNotifier.notifyReceivers(_acceptedSocket);
		
		my(Threads.class).sleepWithoutInterruptions(5000);
		 
		assertNotNull(contactManager.contactGiven("Neide"));
	}
}