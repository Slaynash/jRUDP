package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.RUDPClient;

public abstract class PacketHandler {
	/* Fields */
	public RUDPClient rudp;
	
	/* Methods */
	public abstract void onConnection();
	
	public abstract void onDisconnectedByLocal(String reason);
	
	public abstract void onDisconnectedByRemote(String reason);
	
	public abstract void onPacketReceived(byte[] data);
	
	public abstract void onReliablePacketReceived(byte[] data);

	public abstract void onRemoteStatsReturned(int sentRemote, int sentRemoteR, int receivedRemote, int receivedRemoteR);
}
