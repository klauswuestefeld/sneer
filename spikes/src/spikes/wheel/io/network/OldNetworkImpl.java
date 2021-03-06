//Copyright (C) 2004 Klaus Wuestefeld
//This is free software. It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the license distributed along with this file for more details.
//Contributions: Alexandre Nodari.

package spikes.wheel.io.network;

import java.io.IOException;


public class OldNetworkImpl implements OldNetwork {

	@Override
	public ObjectSocket openSocket(String serverIpAddress, int serverPort) throws IOException {
		return new ObjectSocketImpl(serverIpAddress, serverPort);
	}

	@Override
	public ObjectServerSocket openObjectServerSocket(int port) throws IOException {
		return new ObjectServerSocketImpl(port);
	}
}
