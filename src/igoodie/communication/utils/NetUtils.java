package igoodie.communication.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Fast values to bytes conversion
 */
public abstract class NetUtils {
	/* Readers */
	public static short asShort(byte[] buffer, int offset) {
		return (short) (
				  ((buffer[offset+0] & 0xFF) << 8) 
				| ((buffer[offset+1] & 0xFF) << 0)
		);
	}

	public static int asInt(byte[] buffer, int offset) {
		return    ((buffer[offset+0] & 0xFF) << 24)
				| ((buffer[offset+1] & 0xFF) << 16) 
				| ((buffer[offset+2] & 0xFF) << 8) 
				| ((buffer[offset+3] & 0xFF) << 0);
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

	public static float asFloat(byte[] buffer, int offset) {
		return Float.intBitsToFloat(
				  ((buffer[offset+0] & 0xFF) << 24)
				| ((buffer[offset+1] & 0xFF) << 16) 
				| ((buffer[offset+2] & 0xFF) << 8) 
				| ((buffer[offset+3] & 0xFF) << 0)
		);
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
	
	/**/
	public static String truncateString(String str, int max) {
		if(str.length() <= max) return str;
		return str.substring(0, max);
	}
	
	public static InetAddress getInternetAdress(String host) {
		try {
			return InetAddress.getByName(host);
		}
		catch(UnknownHostException e) {
			return null;
		}
	}
}
