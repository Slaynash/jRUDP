package fr.slaynash.communication.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Fast values to bytes conversion
 */
public abstract class NetUtils {
	/* Readers */
	public static short asShort(byte[] buffer) {
		return asShort(buffer, 0);
	}
	
	public static short asShort(byte[] buffer, int offset) {
		return (short) (
				  ((buffer[offset+0] & 0xFF) << 8) 
				| ((buffer[offset+1] & 0xFF) << 0)
		);
	}
	
	public static int asInt(byte[] buffer) {
		return asInt(buffer, 0);
	}
	
	public static int asInt(byte[] buffer, int offset) {
		return    ((buffer[offset+0] & 0xFF) << 24)
				| ((buffer[offset+1] & 0xFF) << 16) 
				| ((buffer[offset+2] & 0xFF) << 8) 
				| ((buffer[offset+3] & 0xFF) << 0);
	}
	
	public static long asLong(byte[] buffer) {
		return asLong(buffer, 0);
	}

	public static long asLong(byte[] buffer, int offset) {
		return    ((long)(buffer[offset+0] & 0xFF) << 56)
				| ((long)(buffer[offset+1] & 0xFF) << 48) 
				| ((long)(buffer[offset+2] & 0xFF) << 40) 
				| ((long)(buffer[offset+3] & 0xFF) << 32)
				| ((long)(buffer[offset+4] & 0xFF) << 24)
				| ((long)(buffer[offset+5] & 0xFF) << 16) 
				| ((long)(buffer[offset+6] & 0xFF) << 8) 
				| ((long)(buffer[offset+7] & 0xFF) << 0);
	}
	
	public static float asFloat(byte[] buffer) {
		return asFloat(buffer, 0);
	}

	public static float asFloat(byte[] buffer, int offset) {
		return Float.intBitsToFloat(
				  ((buffer[offset+0] & 0xFF) << 24)
				| ((buffer[offset+1] & 0xFF) << 16) 
				| ((buffer[offset+2] & 0xFF) << 8) 
				| ((buffer[offset+3] & 0xFF) << 0)
		);
	}
	
	public static double asDouble(byte[] buffer) {
		return asDouble(buffer, 0);
	}

	public static double asDouble(byte[] buffer, int offset) {
		return Double.longBitsToDouble(
				  ((long)(buffer[offset+0] & 0xFF) << 56)
				| ((long)(buffer[offset+1] & 0xFF) << 48) 
				| ((long)(buffer[offset+2] & 0xFF) << 40) 
				| ((long)(buffer[offset+3] & 0xFF) << 32)
				| ((long)(buffer[offset+4] & 0xFF) << 24)
				| ((long)(buffer[offset+5] & 0xFF) << 16) 
				| ((long)(buffer[offset+6] & 0xFF) << 8) 
				| ((long)(buffer[offset+7] & 0xFF) << 0)
		);
	}

	/* Writers */
	public static void writeBytes(byte[] buffer, int offset, short num){
		buffer[offset  ] = (byte) (num >> 8);
		buffer[offset+1] = (byte) (num >> 0);
	}
	
	public static void writeBytes(byte[] buffer, int offset, int num){
		buffer[offset  ] = (byte) (num >> 24);
		buffer[offset+1] = (byte) (num >> 16);
		buffer[offset+2] = (byte) (num >> 8);
		buffer[offset+3] = (byte) (num >> 0);
	}
	
	public static void writeBytes(byte[] buffer, int offset, long num){
		buffer[offset  ] = (byte) (num >> 56);
		buffer[offset+1] = (byte) (num >> 48);
		buffer[offset+2] = (byte) (num >> 40);
		buffer[offset+3] = (byte) (num >> 32);
		buffer[offset+4] = (byte) (num >> 24);
		buffer[offset+5] = (byte) (num >> 16);
		buffer[offset+6] = (byte) (num >> 8);
		buffer[offset+7] = (byte) (num >> 0);
	}

	public static void writeBytes(byte[] buffer, int offset, float num){
		int i = Float.floatToIntBits(num);
		buffer[offset  ] = (byte) (i >> 24);
		buffer[offset+1] = (byte) (i >> 16);
		buffer[offset+2] = (byte) (i >> 8);
		buffer[offset+3] = (byte) (i >> 0);
	}
	
	public static void writeBytes(byte[] buffer, int offset, double num){
		long l = Double.doubleToLongBits(num);
		buffer[offset  ] = (byte) (l >> 56);
		buffer[offset+1] = (byte) (l >> 48);
		buffer[offset+2] = (byte) (l >> 40);
		buffer[offset+3] = (byte) (l >> 32);
		buffer[offset+4] = (byte) (l >> 24);
		buffer[offset+5] = (byte) (l >> 16);
		buffer[offset+6] = (byte) (l >> 8);
		buffer[offset+7] = (byte) (l >> 0);
	}
	
	/* Truncators */
	public static String truncateString(String str, int max) {
		if(str.length() <= max) return str;
		return str.substring(0, max); //Truncate rightmost chars
	}
	
	public static int truncateToInt(long num) {
		String s1 = num + "";
		return Integer.parseInt(s1.substring(Math.max(s1.length() - 9, 0)));
		//Evil digit truncator from long to int.
		//Might be a little bit slow, but there is no way to do it as we know
	}
	
	/**/
	public static InetAddress getInternetAdress(String host) {
		try {
			return InetAddress.getByName(host);
		}
		catch(UnknownHostException e) {
			return null;
		}
	}

	public static String asHexString(byte[] source) {
		if(source.length == 0) return "";
		StringBuilder sb = new StringBuilder("0x");
		
		for(int i=0; i<source.length; i++) {
			sb.append(String.format("%02X", source[i]));
			if(i != source.length-1) sb.append("_");
		}
		
		return sb.toString().trim();
	}
	
	public static String asHexString(long num) {
		String hex = String.format("%016X", num);
		StringBuilder sb = new StringBuilder("0x");
		
		for(int i=0; i<hex.length()-4; i+=4) {
			sb.append(hex.substring(i, i+4) + "_");
		}
		sb.append(hex.substring(12, 16));
		
		return sb.toString();
	}
	
	public static String asHexString(int num) {
		String hex = String.format("%08X", num);
		StringBuilder sb = new StringBuilder("0x");
		
		for(int i=0; i<hex.length()-4; i+=4) {
			sb.append(hex.substring(i, i+4) + "_");
		}
		sb.append(hex.substring(4, 8));
		
		return sb.toString();
	}
	
	public static String asBinString(byte[] source) {
		if(source.length == 0) return "";
		StringBuilder sb = new StringBuilder("0b");
		
		for(int i=0; i<source.length; i++) {
			sb.append(String.format("%8s", Integer.toBinaryString(source[i])));
			if(i != source.length-1) sb.append("_");
		}
		
		return sb.toString().trim().replace(" ", "0");
	}
	
	public static byte[] getPacketPayloadFromRaw(byte[] rawpacket) {
		byte[] packetData = new byte[rawpacket.length - 9];
		System.arraycopy(rawpacket, 9, packetData, 0, packetData.length);
		return packetData;
	}
	
	public static boolean sequence_greater_than( short s1, short s2 ){
        return ( ( s1 > s2 ) && ( s1 - s2 <= 32768 ) ) || 
               ( ( s1 < s2 ) && ( s2 - s1  > 32768 ) );
    }

}
