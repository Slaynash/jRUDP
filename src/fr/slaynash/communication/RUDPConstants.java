package fr.slaynash.communication;

public class RUDPConstants {
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
	public static final int VERSION_MINOR = 4;
	
	public static final int START_COMMAND_BYTES = -119;
	public static final long PING_INTERVAL = 1000;
	
	public static class PacketType {
		public static final byte UNRELIABLE = 0;
		public static final byte RELIABLE = 1;
		public static final byte HANDSHAKE_START = 2;
		public static final byte HANDSHAKE_OK = 3;
		public static final byte HANDSHAKE_ERROR = 4;
	}

	public static class Commands {
		//public static final byte HANDSHAKE_START = -128; // now in first byte
		//public static final byte HANDSHAKE_OK = -127; // now in first byte
		//public static final byte HANDSHAKE_ERROR = -126; // now in first byte
		public static final byte PING_REQUEST = -125;
		public static final byte PING_RESPONSE = -124;
		public static final byte DISCONNECT = -123;
		public static final byte RELY = -122;
		public static final byte PACKETSSTATS_REQUEST = -121;
		public static final byte PACKETSSTATS_RESPONSE = -120;
	}
}
