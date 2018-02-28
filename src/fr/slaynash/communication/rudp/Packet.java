package fr.slaynash.communication.rudp;

import fr.slaynash.communication.RUDPConstants;
import fr.slaynash.communication.utils.NetUtils;

/**
 * Simple packet definition to be extended by other packet types
 * @author iGoodie
 */
public abstract class Packet { //TODO impl the base

	public static class PacketHeader {		
		private boolean isReliable = false;
		private int truncNs = -1;

		public boolean isReliable() {
			return isReliable;
		}

		public int getReceivedNs() {
			return truncNs;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Reliable:" + isReliable + ", ");
			sb.append("ReceivedNS:" + truncNs);
			return sb.toString();
		}
	}

	private PacketHeader header = new PacketHeader();

	private byte[] rawPayload;

	public Packet(byte[] data) {
		//Parse header
		header.isReliable = data[0] == RUDPConstants.PacketType.RELIABLE;
		header.truncNs = NetUtils.asInt(data, 1);

		//Parse payload
		rawPayload = new byte[data.length - 3];
		System.arraycopy(data, 5, rawPayload, 0, rawPayload.length);
	}

	/* Getter and Setters */
	public PacketHeader getHeader() {
		return header;
	}

	/* Overrides */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("h{");
		sb.append(header);
		sb.append("} p{");
		sb.append(NetUtils.asHexString(rawPayload));
		sb.append("}");
		return sb.toString();
	}
}
