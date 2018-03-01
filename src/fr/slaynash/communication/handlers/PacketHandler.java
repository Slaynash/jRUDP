package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.RUDPClient;

public abstract class PacketHandler {
	/* Fields */
	public RUDPClient rudp;
	
	/* Constructors */
	public PacketHandler(RUDPClient rudpClient) {
		this.rudp = rudpClient;
	}
	
	/* Methods */
	public abstract void initializeClient();
	
	public abstract void onDisconnected(String reason);
	
	public void disconnect(String reason){
		rudp.disconnect(reason);
	}
	
	public abstract void onPacketReceived(byte[] data);
	
	public abstract void onReliablePacketReceived(byte[] data);

	public abstract void onRemoteStatsReturned(int sentRemote, int sentRemoteR, int receivedRemote, int receivedRemoteR);
}
