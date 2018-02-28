package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.utils.PacketQueue;

public class ClientPHandler extends PacketHandler {
	
	private PacketQueue reliableQueue = new PacketQueue();

	public ClientPHandler(RUDPClient rudpClient) {
		super(rudpClient);
	}

	@Override
	public void initializeClient() {}

	@Override
	public void onDisconnected(String reason) {}

	@Override
	public void handlePacket(byte[] data) {}

	@Override
	public void handleReliablePacket(byte[] data) {
		Packet packet = new Packet(data) {}; //Parse packet
		//if(packet.getHeader().ns)
		//TODO impl handle after design change
	}
}
