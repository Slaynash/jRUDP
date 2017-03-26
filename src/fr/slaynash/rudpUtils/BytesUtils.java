package fr.slaynash.rudpUtils;

public class BytesUtils {
	/*
	public static long toLong(byte[] bytes) {
		return    ((bytes[0] & 0xFF) << 56)
				| ((bytes[1] & 0xFF) << 48) 
				| ((bytes[2] & 0xFF) << 40) 
				| ((bytes[3] & 0xFF) << 32)
				| ((bytes[4] & 0xFF) << 24)
				| ((bytes[5] & 0xFF) << 16) 
				| ((bytes[6] & 0xFF) << 8) 
				| ((bytes[7] & 0xFF) << 0);
	}
	*/
	public static long toLong(byte[] b) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (b[i] & 0xFF);
	    }
	    return result;
	}
}
