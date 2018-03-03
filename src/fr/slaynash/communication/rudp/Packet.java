package fr.slaynash.communication.rudp;

import fr.slaynash.communication.RUDPConstants;
import fr.slaynash.communication.utils.NetUtils;

/**
 * Simple packet definition to be extended by other packet types
 * @author iGoodie
 */
public abstract class Packet {

	public static final int HEADER_SIZE = 3; //bytes
	
	public static class PacketHeader {		
		private boolean isReliable = false;
		private short sequenceNum;

		public boolean isReliable() {
			return isReliable;
		}

		public short getSequenceNo() {
			return sequenceNum;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Reliable:" + isReliable + ", ");
			sb.append("SequenceNo:" + sequenceNum);
			return sb.toString();
		}
	}

	/* Fields*/
	private PacketHeader header = new PacketHeader();
	private byte[] rawPayload;

	/* Constructor */
	public Packet(byte[] data) {
		//Parse header
		header.isReliable = RUDPConstants.isPacketReliable(data[0]);
		header.sequenceNum = NetUtils.asShort(data, 1);

		//Parse payload
		rawPayload = new byte[data.length - HEADER_SIZE];
		System.arraycopy(data, HEADER_SIZE, rawPayload, 0, rawPayload.length);
	}

	/* Getter and Setters */
	public PacketHeader getHeader() {
		return header;
	}

	public byte[] getRawPayload() {
		return rawPayload;
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
