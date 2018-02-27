package fr.slaynash.communication;

public class RUDPConstants {
	public static final int RECEIVE_MAX_SIZE = 4096;
	public static final int CLIENT_TIMEOUT_TIME = 5000;
	public static final long CLIENT_TIMEOUT_TIME_NANOSECONDS = 5000000000L;//5,000,000,000 nanoseconds = 5 seconds
	public static final long PACKET_TIMEOUT_TIME_NANOSECONDS = 5000000000L;
	
	public static final int VERSION_MAJOR = 1;
	public static final int VERSION_MINOR = 4;
	
	public static final int START_COMMAND_BYTES = -119;
	public static final long PING_INTERVAL = 1000;
	
	public static final byte UNRELIABLE = 0;
	public static final byte RELIABLE = 1;

	public static class Commands {
		public static final byte HANDSHAKE_START = -128;
		public static final byte HANDSHAKE_OK = -127;
		public static final byte HANDSHAKE_ERROR = -126;
		public static final byte PING_REQUEST = -125;
		public static final byte PING_RESPONSE = -124;
		public static final byte DISCONNECT = -123;
		public static final byte RELY = -122;
		public static final byte PACKETSSTATS_REQUEST = -121;
		public static final byte PACKETSSTATS_RESPONSE = -120;
	}
}
