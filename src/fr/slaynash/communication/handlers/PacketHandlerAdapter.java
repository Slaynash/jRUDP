package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.RUDPClient;

public class PacketHandlerAdapter extends PacketHandler {

	public PacketHandlerAdapter(RUDPClient rudpClient) {
		super(rudpClient);
	}

	@Override
	public void initializeClient() {}

	@Override
	public void onDisconnectedByLocal(String reason) {}

	@Override
	public void onDisconnectedByRemote(String reason) {}

	@Override
	public void onPacketReceived(byte[] data) {}

	@Override
	public void onReliablePacketReceived(byte[] data) {}

	@Override
	public void onRemoteStatsReturned(int sentRemote, int sentRemoteR, int receivedRemote, int receivedRemoteR) {}

}
