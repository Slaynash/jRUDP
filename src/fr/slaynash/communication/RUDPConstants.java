package fr.slaynash.communication;

public final class RUDPConstants {
	public static final int RECEIVE_MAX_SIZE = 4096;
	public static final int CLIENT_TIMEOUT_TIME = 5000;
	
	/**
	 * maximum time between ping packets before disconnecting client
	 */
	public static final long CLIENT_TIMEOUT_TIME_MILLISECONDS = 5000L;
	
	/**
	 * Packet's time before dropping it
	 */
	public static final long PACKET_TIMEOUT_TIME_MILLISECONDS = 5000L;
	
	/**
	 * Packet's seq store time after being received (used to avoid duplicate packets)
	 */
	public static final long PACKET_STORE_TIME_MILLISECONDS = 2000L;
	
	public static final int VERSION_MAJOR = 1;
	public static final int VERSION_MINOR = 0;
	
	public static final long PING_INTERVAL = 1000;
	
	public static class PacketType {
		public static final byte UNRELIABLE				= createPacketType((byte)0 , false);
		public static final byte RELIABLE				= createPacketType((byte)1 , true );
		public static final byte HANDSHAKE_START		= createPacketType((byte)2 , false);
		public static final byte HANDSHAKE_OK			= createPacketType((byte)3 , false);
		public static final byte HANDSHAKE_ERROR		= createPacketType((byte)4 , false);
		public static final byte PING_REQUEST			= createPacketType((byte)5 , false);
		public static final byte PING_RESPONSE			= createPacketType((byte)6 , false);
		public static final byte DISCONNECT_FROMCLIENT	= createPacketType((byte)7 , false);
		public static final byte DISCONNECT_FROMSERVER	= createPacketType((byte)8 , true );
		public static final byte RELY					= createPacketType((byte)9 , false);
		public static final byte PACKETSSTATS_REQUEST	= createPacketType((byte)10, false);
		public static final byte PACKETSSTATS_RESPONSE	= createPacketType((byte)11, false);
	}
	
	private static byte createPacketType(byte id, boolean reliable) {
		if((id & 0b1000_0000) == 0b1000_0000)
			throw new IllegalArgumentException("Packet id too big for adding the reliability bit");
		if(reliable)
			return (byte) (id | (1 << 7));
		return id;
	}
	
	public static boolean isPacketReliable(int packetType) {
		return (packetType & 0b1000_0000) == 0b1000_0000;
	}
}
