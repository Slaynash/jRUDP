package fr.slaynash.communication.rudp;

public class Values {
	public static final int RECEIVE_MAX_SIZE = 8096;
	public static final int CLIENT_TIMEOUT_TIME = 5000;
	public static final long CLIENT_TIMEOUT_TIME_NANOSECONDS = 5000000000L;//5,000,000,000 nanoseconds = 5 seconds
	public static final long PACKET_TIMEOUT_TIME_NANOSECONDS = 5000000000L;
	public static final int VERSION_MAJOR = 1;
	public static final int VERSION_MINOR = 1;
	public static final int RESEVED_COMMAND_BYTES = 7;
	
	
	public static class connectionStates{
		public static final int STATE_DISCONNECTED = 0;
		public static final int STATE_CONNECTING = 1;
		public static final int STATE_CONNECTED = 2;
		public static final int STATE_DISCONNECTING = 3;
	}


	public static class commands{
		public static final byte HANDSHAKE_START = 0x01;
		public static final byte HANDSHAKE_OK = 0x02;
		public static final byte HANDSHAKE_ERROR = 0x03;
		public static final byte PING_REQUEST = 0x04;
		public static final byte PING_RESPONSE = 0x05;
		public static final byte DISCONNECT = 0x06;
		public static final byte RELY = 0x07;
	}
	
	public static class clientType{
		public static int NORMAL_CLIENT = 0;
		public static int SERVER_CHILD = 1;
	}
}
