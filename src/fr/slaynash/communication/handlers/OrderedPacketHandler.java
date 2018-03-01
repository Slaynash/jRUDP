package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.utils.NetUtils;
import fr.slaynash.communication.utils.PacketQueue;

public class OrderedPacketHandler extends PacketHandler {
	
	protected PacketQueue reliableQueue = new PacketQueue();
	protected short lastHandledSeq = Short.MAX_VALUE;

	public OrderedPacketHandler(RUDPClient rudpClient) {
		super(rudpClient);
	}

	@Override
	public void initializeClient() {}

	@Override
	public void onDisconnected(String reason) {
		reliableQueue = new PacketQueue();
		lastHandledSeq = Short.MAX_VALUE;
	}

	@Override
	public void onPacketReceived(byte[] data) {}

	@Override
	public void onRemoteStatsReturned(int sentRemote, int sentRemoteR, int receivedRemote, int receivedRemoteR) {}

	@Override
	public void onReliablePacketReceived(byte[] data) {
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
		System.out.println("Received: " + packet); //Print packet just to test
	}
}
