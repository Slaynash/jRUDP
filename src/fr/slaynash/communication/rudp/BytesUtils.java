package fr.slaynash.communication.rudp;

/**
 * Fast values to bytes conversion
 */
public abstract class BytesUtils {
	
	public static void writeBytes(byte[] buffer, int offset, short s){
		buffer[offset  ] = (byte) (s >> 8);
		buffer[offset+1] = (byte) (s >> 0);
	}
	
	public static void writeBytes(byte[] buffer, int offset, int i){
		buffer[offset  ] = (byte) (i >> 24);
		buffer[offset+1] = (byte) (i >> 16);
		buffer[offset+2] = (byte) (i >> 8);
		buffer[offset+3] = (byte) (i >> 0);
	}
	
	public static void writeBytes(byte[] buffer, int offset, long i){
		buffer[offset  ] = (byte) (i >> 56);
		buffer[offset+1] = (byte) (i >> 48);
		buffer[offset+2] = (byte) (i >> 40);
		buffer[offset+3] = (byte) (i >> 32);
		buffer[offset+4] = (byte) (i >> 24);
		buffer[offset+5] = (byte) (i >> 16);
		buffer[offset+6] = (byte) (i >> 8);
		buffer[offset+7] = (byte) (i >> 0);
	}

	public static void writeBytes(byte[] buffer, int offset, float f){
		int i = Float.floatToIntBits(f);
		buffer[offset  ] = (byte) (i >> 24);
		buffer[offset+1] = (byte) (i >> 16);
		buffer[offset+2] = (byte) (i >> 8);
		buffer[offset+3] = (byte) (i >> 0);
	}
	
	public static void writeBytes(byte[] buffer, int offset, double d){
		long l = Double.doubleToLongBits(d);
		buffer[offset  ] = (byte) (l >> 56);
		buffer[offset+1] = (byte) (l >> 48);
		buffer[offset+2] = (byte) (l >> 40);
		buffer[offset+3] = (byte) (l >> 32);
		buffer[offset+4] = (byte) (l >> 24);
		buffer[offset+5] = (byte) (l >> 16);
		buffer[offset+6] = (byte) (l >> 8);
		buffer[offset+7] = (byte) (l >> 0);
	}
	
	public static int toShort(byte[] buffer, int offset) {
		return    ((buffer[offset+0] & 0xFF) << 8) 
				| ((buffer[offset+1] & 0xFF) << 0);
	}

	public static int toInt(byte[] buffer, int offset) {
		return    ((buffer[offset+0] & 0xFF) << 24)
				| ((buffer[offset+1] & 0xFF) << 16) 
				| ((buffer[offset+2] & 0xFF) << 8) 
				| ((buffer[offset+3] & 0xFF) << 0);
	}

	public static long toLong(byte[] buffer, int offset) {
		return    ((buffer[offset+0] & 0xFF) << 56)
				| ((buffer[offset+1] & 0xFF) << 48) 
				| ((buffer[offset+2] & 0xFF) << 40) 
				| ((buffer[offset+3] & 0xFF) << 32)
				| ((buffer[offset+4] & 0xFF) << 24)
				| ((buffer[offset+5] & 0xFF) << 16) 
				| ((buffer[offset+6] & 0xFF) << 8) 
				| ((buffer[offset+7] & 0xFF) << 0);
	}

	public static float toFloat(byte[] buffer, int offset) {
		return Float.intBitsToFloat(
				  ((buffer[offset+0] & 0xFF) << 24)
				| ((buffer[offset+1] & 0xFF) << 16) 
				| ((buffer[offset+2] & 0xFF) << 8) 
				| ((buffer[offset+3] & 0xFF) << 0)
		);
	}

	public static double toDouble(byte[] buffer, int offset) {
		return Double.longBitsToDouble(
				  ((buffer[offset+0] & 0xFF) << 56)
				| ((buffer[offset+1] & 0xFF) << 48) 
				| ((buffer[offset+2] & 0xFF) << 40) 
				| ((buffer[offset+3] & 0xFF) << 32)
				| ((buffer[offset+4] & 0xFF) << 24)
				| ((buffer[offset+5] & 0xFF) << 16) 
				| ((buffer[offset+6] & 0xFF) << 8) 
				| ((buffer[offset+7] & 0xFF) << 0)
		);
	}
}
