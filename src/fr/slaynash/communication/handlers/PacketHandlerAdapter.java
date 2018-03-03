package fr.slaynash.communication.handlers;

public class PacketHandlerAdapter extends PacketHandler {

	@Override
	public void onConnection() {}

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
