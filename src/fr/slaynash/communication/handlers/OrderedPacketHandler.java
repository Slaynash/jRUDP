package fr.slaynash.communication.handlers;

import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.utils.NetUtils;
import fr.slaynash.communication.utils.PacketQueue;

public class OrderedPacketHandler extends PacketHandler {
	
	protected PacketQueue reliableQueue = new PacketQueue();
	protected short lastHandledSeq = -1;

	@Override
	public void onConnection() {}

	@Override
	public void onDisconnectedByRemote(String reason) {
		reliableQueue = new PacketQueue();
		lastHandledSeq = Short.MAX_VALUE;
	}
	
	@Override
	public void onDisconnectedByLocal(String reason) {
		reliableQueue = new PacketQueue();
		lastHandledSeq = Short.MAX_VALUE;
	}

	@Override
	public void onPacketReceived(byte[] data) {}

	@Override
	public void onRemoteStatsReturned(int sentRemote, int sentRemoteR, int receivedRemote, int receivedRemoteR) {}

	@Override
	public void onReliablePacketReceived(byte[] data) {
		Packet packet = new Packet(data) {}; //Parse received packet
		short expectedSeq = NetUtils.shortIncrement(lastHandledSeq); //last + 1
		
		if(NetUtils.sequence_greater_than(lastHandledSeq, packet.getHeader().getSequenceNo())) { // (last > received) == (received < last)
			return; // Drop the packet, because we already handled it
		}
		
		//Received an unexpected packet? Enqueue and pass
		if(packet.getHeader().getSequenceNo() != expectedSeq) { 
			reliableQueue.enqueue(packet);
			return;
		}
		
		// Handle expected packet
		onExpectedPacketReceived(packet); 
		lastHandledSeq = packet.getHeader().getSequenceNo();
		expectedSeq = NetUtils.shortIncrement(lastHandledSeq); 

		// Handle every waiting packet
		while(!reliableQueue.isEmpty() && reliableQueue.peek().getHeader().getSequenceNo() == expectedSeq) {
			packet = reliableQueue.dequeue();
			onExpectedPacketReceived(packet);
			lastHandledSeq = expectedSeq;
			expectedSeq = NetUtils.shortIncrement(lastHandledSeq);			
		}
	}
	
	public void onExpectedPacketReceived(Packet packet) {
		System.out.println("Handling: " + packet); //Print packet just to test
	}
	
	/*
	//Reliability ordering test
	public static void main(String[] args) {
		OrderedPacketHandler handler = new OrderedPacketHandler();
		byte[][] list = new byte[][] {
			{0x1, 0b0000_0000, 0b0000_0000}, //0 *
			{0x1, 0b0000_0000, 0b0000_0001}, //1 *
			{0x1, 0b0000_0000, 0b0000_0011}, //3
			{0x1, 0b0000_0000, 0b0000_0101}, //5
			{0x1, 0b0000_0000, 0b0000_0100}, //4 *
			{0x1, 0b0000_0000, 0b0000_0111}, //7
			{0x1, 0b0000_0000, 0b0000_0010}, //2 *
			{0x1, 0b0000_0000, 0b0000_0110}, //6
			{0x1, 0b0000_0000, 0b0000_1000}, //8 *
		};
		
		for(byte[] p_data : list) {
			handler.onReliablePacketReceived(p_data);
		}
	}*/
}
