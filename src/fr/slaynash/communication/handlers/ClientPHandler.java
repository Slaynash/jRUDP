package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.utils.NetUtils;
import fr.slaynash.communication.utils.PacketQueue;

public class ClientPHandler extends PacketHandler {
	
	private PacketQueue reliableQueue = new PacketQueue();
	private short lastHandledSeq = Short.MAX_VALUE;

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
	public void receiveReliablePacket(byte[] data) {
		Packet packet = new Packet(data) {}; //Parse packet
		short inc = NetUtils.shortIncrement(lastHandledSeq); //last+1
		
		if(packet.getHeader().getSequenceNo() == inc) { //Expected packet received!
			do {
				handleReliablePacketOrdered(packet); //Handle it! Handle it!
				lastHandledSeq = inc; //We handled it, lets keep that in mind
			} while(!reliableQueue.isEmpty() && reliableQueue.peek().getHeader().getSequenceNo() == lastHandledSeq); 
			// ^ If we have packets waiting to be handled and next one is right there, keep handling them!
		}
		else { //Uh oh some wrong ordered packet arrived :c
			reliableQueue.enqueue(packet); //Queue it and lets wait for the expected one :c
		}
	}
	
	public void handleReliablePacketOrdered(Packet packet) {
		System.out.println(packet); //Print packet just to test
	}
}
