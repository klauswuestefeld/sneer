package sneer.bricks.connection.impl;

import sneer.bricks.connection.SocketAccepter;
import sneer.bricks.connection.SocketReceiver;
import sneer.bricks.network.ByteArraySocket;
import sneer.lego.Brick;
import sneer.lego.Container;
import sneer.lego.Startable;
import wheel.lang.Omnivore;
import wheel.lang.Threads;

public class SocketReceiverImpl implements SocketReceiver, Startable {

	@Brick
	private SocketAccepter _socketAccepter;
	
	@Brick
	private Container _container;
	
	@Override
	public void start() throws Exception {
		_socketAccepter.lastAcceptedSocket().addReceiver(new Omnivore<ByteArraySocket>() { @Override public void consume(final ByteArraySocket socket) {
			Threads.startDaemon(new Runnable(){@Override public void run() {
				_container.create(IndividualSocketReceiver.class,socket);
			}});
		}});
	}

}
