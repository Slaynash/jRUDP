package fr.slaynash.communication.utils;

import java.util.Comparator;
import java.util.PriorityQueue;

import fr.slaynash.communication.rudp.Packet;

/**
 * Priority queue wrapper for packet definitions
 * @author iGoodie
 */
public class PacketQueue {
	public static class PacketNSComparator implements Comparator<Packet> {
		@Override
		public int compare(Packet o1, Packet o2) {
			if(o1.getHeader().getSequenceNo() == o2.getHeader().getSequenceNo()) return 0;
			return o1.getHeader().getSequenceNo() < o2.getHeader().getSequenceNo() ? -1 : 1;
		}
	}
	
	private PriorityQueue<Packet> packetQueue;
	
	public PacketQueue() {
		packetQueue = new PriorityQueue<>(new PacketNSComparator());
	}

	public void enqueue(Packet packet) {
		packetQueue.add(packet);
	}
	
	public Packet dequeue() {
		return packetQueue.isEmpty() ? null : packetQueue.remove();
	}
	
	public Packet peek() {
		return packetQueue.peek();
	}

	public int size() {
		return packetQueue.size();
	}
	
	public boolean isEmpty() {
		return packetQueue.isEmpty();
	}

	/*public static void main(String[] args) { //Test queue
		Packet p1 = new Packet(new byte[]{0x01, 0x00, 0x00, 0x00, 0x10, 0x7F}) {};
		Packet p2 = new Packet(new byte[]{0x01, 0x00, 0x00, 0x00, 0x01}) {};
		Packet p3 = new Packet(new byte[]{0x01, 0x00, 0x00, 0x10, 0x01}) {};
		System.out.println(p1);
		System.out.println(p2);
		System.out.println(p3);
		
		PacketQueue pq = new PacketQueue();
		pq.enqueue(p1);
		pq.enqueue(p2);
		pq.enqueue(p3);
		
		System.out.println();
		
		while((p1=pq.dequeue()) != null) {
			System.out.println(p1);
		}
	}*/
}
