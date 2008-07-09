package sneer.bricks.mesh.impl;

import java.io.NotSerializableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import sneer.bricks.connection.Connection;
import sneer.bricks.connection.ConnectionManager;
import sneer.bricks.contacts.Contact;
import sneer.bricks.keymanager.KeyManager;
import sneer.bricks.keymanager.PublicKey;
import sneer.bricks.log.Logger;
import sneer.bricks.mesh.Me;
import sneer.bricks.mesh.Party;
import sneer.bricks.serialization.Serializer;
import sneer.bricks.threadpool.ThreadPool;
import sneer.lego.Brick;
import sneer.lego.Inject;
import wheel.lang.Omnivore;
import wheel.lang.Types;
import wheel.reactive.Signal;
import wheel.reactive.lists.ListSignal;
import wheel.reactive.lists.impl.SimpleListReceiver;

class SignalConnection implements Visitable {

	@Inject
	static private ConnectionManager _connectionManager;
	
	@Inject
	static private Serializer _serializer;

	@Inject
	static private ThreadPool _threadPool;

	@Inject
	static private KeyManager _keyManager;
	
	@Inject
	static private Logger _logger;

	private final Connection _connection;

	private final PriorityQueue<byte[]> _priorityQueue = new PriorityQueue<byte[]>(10);

	private volatile boolean _isCrashed = false;

	private List<Object> _scoutsToAvoidGC = new ArrayList<Object>();



	SignalConnection(Contact contact) {
		_connection = _connectionManager.connectionFor(contact);
		_connection.setReceiver(new Omnivore<byte[]>(){public void consume(byte[] packetReceived) {
			receive(packetReceived);
		}});
		startSender();
	}
	
	private void receive(byte[] packetReceived) {
		Object candidate;
		try {
			candidate = _serializer.deserialize(packetReceived, SignalConnection.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new wheel.lang.exceptions.NotImplementedYet(e); // Fix Handle this exception.
		}

		Ambassador ambassador;
		try {
			ambassador = (Ambassador)candidate;
		} catch (ClassCastException e) {
			throw new wheel.lang.exceptions.NotImplementedYet(e); // Fix Handle this exception.
		}

		ambassador.visit(this);
	}

	private void startSender() {
		_threadPool.registerActor(new Runnable() { public void run() {
			while (!_isCrashed) {
				byte[] toSend = _priorityQueue.waitForNext();
				_connection.send(toSend);
			}
		}});
	}

	private void send(Object object) {
		byte[] packet = serialize(object);

		_priorityQueue.add(packet, 2);
	}


	private byte[] serialize(Object object) {
		try {
			return _serializer.serialize(object);
		} catch (NotSerializableException e) {
			throw new RuntimeException(e);
		}
	}

	void crash() {
		_isCrashed = true;
	}


	private Omnivore<Object> createScoutFor(final PublicKey publicKey, final Class<? extends Brick> brickInterface, final String signalPath) {
		Omnivore<Object> result = new Omnivore<Object>() {@Override public void consume(Object notification) {
			send(new Notification(publicKey, brickInterface, signalPath, notification));
		}};
		_scoutsToAvoidGC.add(result); //Fix: This is a Leak.
		return result;
	}

	private void createScoutForContacts(final PublicKey publicKey, ListSignal<Contact> contacts) {
		SimpleListReceiver<Contact> scout = new SimpleListReceiver<Contact>(contacts) {

			@Override
			protected void elementPresent(Contact contact) {
				elementAdded(contact);
			}
			
			@Override
			protected void elementAdded(Contact contact) {
				maintainChainOfIntermediaries(publicKey, contact);
				addNicknameScout(publicKey, contact);
			}

			@Override
			protected void elementToBeRemoved(Contact contact) {
				send(new NotificationOfContactRemoved(publicKey, toRemoteContact(contact)));
			}
		};

		_scoutsToAvoidGC.add(scout); //Fix: This is a Leak.
	}

	private void addNicknameScout(final PublicKey publicKey, final Contact contact) {
		Omnivore<String> scout = new Omnivore<String>(){@Override public void consume(String nickname) {
			send(new NotificationOfContact(publicKey, toRemoteContact(contact)));
		}};
		contact.nickname().addReceiver(scout);
		_scoutsToAvoidGC.add(scout);
	}

	private void maintainChainOfIntermediaries(final PublicKey publicKey, Contact contact) {
		AbstractParty intermediary = produceParty(publicKey);
		PublicKey contactPK = intermediary.producePublicKeyFor(contact);
		AbstractParty contactProxy = (AbstractParty) _keyManager.partyGiven(contactPK, ProxyFactory.INSTANCE);
		contactProxy.addIntermediaryIfNecessary(intermediary);
	}

	private RemoteContact toRemoteContact(Contact contact) {
		if (contact instanceof RemoteContact) return (RemoteContact)contact;
		
		PublicKey pk = _keyManager.keyGiven(contact);
		return new RemoteContact(pk, contact.nickname().currentValue());
	}

	private AbstractParty produceParty(PublicKey pk) {
		return (AbstractParty)_keyManager.partyGiven(pk, ProxyFactory.INSTANCE);
	}


	@Override
	public void handleNotification(PublicKey publicKey, Class<? extends Brick> brickInterface, String signalPath, Object newValue) {
		PeerProxy target = produceProxy(publicKey);
		if (target == null) return;
		
		target.handleNotification(brickInterface, signalPath, newValue);
	}

	@Override
	public void handleNotificationOfContactAdded(PublicKey publicKey, RemoteContact newContact) {
		PeerProxy target = produceProxy(publicKey);
		if (target == null) return;
		
		target.handleNotificationOfContact(newContact);
	}
	
	@Override
	public void handleNotificationOfContactRemoved(PublicKey publicKey,	RemoteContact contact) {
		PeerProxy target = produceProxy(publicKey);
		if (target == null) return;
		
		target.handleNotificationOfContactRemoved(contact);
	}
	

	private PeerProxy produceProxy(PublicKey publicKey) {
		AbstractParty target = produceParty(publicKey);
		if (target instanceof Me) {
			_logger.info("Illegal notification.");
			return null;
		}
		return (PeerProxy)target;
	}
	

	void subscribeTo(PublicKey targetPK, String remoteSignalPath) {
		send(new Subscription(targetPK, null, remoteSignalPath));
	}

	void subscribeToContacts(PublicKey targetPK) {
		send(new SubscriptionToContacts(targetPK));
	}

	@Override
	public void serveSubscriptionTo(PublicKey publicKey, Class<? extends Brick> brickInterface, String signalName) {
		Party target = produceParty(publicKey);
		
		Signal<Object> signal;
		Brick brick = target.brickProxyFor(brickInterface);
		signal = invokeSignal(brick, brickInterface, signalName);
				
		signal.addReceiver(createScoutFor(publicKey, brickInterface, signalName));
	}
	
	private Signal<Object> invokeSignal(Brick brick, Class<? extends Brick> correspondingBrickInterface, String signalName) {
		try {
			return tryToInvokeSignal(brick, correspondingBrickInterface, signalName);
		} catch (Exception e) {
			throw new wheel.lang.exceptions.NotImplementedYet(e); // Implement Handle this exception.
		}
	}

	private Signal<Object> tryToInvokeSignal(Brick brick, Class<? extends Brick> correspondingBrickInterface, String signalName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = correspondingBrickInterface.getMethod(signalName, (Class<?>[])null);
		return Types.uncheckedGenericCast(
			method.invoke(brick, (Object[])null));
	}

	@Override
	public void serveSubscriptionToContacts(PublicKey publicKey) {
		Party target = produceParty(publicKey);
		ListSignal<Contact> contacts = target.contacts();

		createScoutForContacts(publicKey, contacts);
	}

	public void subscribeTo(PublicKey targetPK, Class<? extends Brick> brickInterface, String signalName) {
		send(new Subscription(targetPK, brickInterface, signalName));
	}

	
}
