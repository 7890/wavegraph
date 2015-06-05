//tb/150117

package ch.lowres.wavegraph;

//big and little endian
//http://www.cs.umd.edu/class/sum2003/cmsc311/Notes/Data/endian.html

//=======================================================
class ToFloat
{
	private static LawEncoding law;

	final static long m16_div=32768-1; //-1+ (2^16)/2
	final static long m24_div=8388608-1; //-1+ (2^24)/2
	final static long m32_div=2147483648L-1; //-1+ (2^32)/2

//=======================================================
	public static float unsignedByte(byte b1)
	{
		return 2*((float)
			(b1 & 0xff)
			/
			255)
			-1f;
	}

//=======================================================
	public static float alaw(byte b1)
	{
		return (float) law.alaw2linear(b1) / m16_div;
	}

//=======================================================
	public static float ulaw(byte b1)
	{
		return (float) law.ulaw2linear(b1) / m16_div;
	}

//=======================================================
	public static float signed16(byte[] sample, boolean isBigEndian)
	{
		return (float) bytesToInt16(sample,0,isBigEndian) / m16_div;
	}

//=======================================================
	public static float signed24(byte[] sample, boolean isBigEndian)
	{
		return (float) bytesToInt24(sample,0,isBigEndian) / m24_div;
	}

//=======================================================
	public static float signed32(byte[] sample, boolean isBigEndian)
	{
		//2^32 =4294967296
		return (float) bytesToInt32(sample,0,isBigEndian) / m32_div;
	}

//http://coweb.cc.gatech.edu/mediaComp-plan/uploads/55/JavaSound.java
/*
 * EVERYTHING INDEXES FROM 0
 * arrays, frames, samples, etc.
 */
   /**
     * Converts 2 successive bytes starting at <code>byteOffset</code> in 
     * <code>buffer</code> to a signed integer sample with 16bit range.
     * <p>
     * For little endian, buffer[byteOffset] is interpreted as low byte,
     * whereas it is interpreted as high byte in big endian.
     * <p> This is a reference function.
     */ 
    public static int bytesToInt16( byte [] buffer, int byteOffset, 
                                    boolean isBigEndian) 
    { 
        return isBigEndian?
            ((buffer[byteOffset]<<8) | (buffer[byteOffset+1] & 0xFF)):
           
            ((buffer[byteOffset+1]<<8) | (buffer[byteOffset] & 0xFF));
    } 

    /**
     * Converts 3 successive bytes starting at <code>byteOffset</code> in 
     * <code>buffer</code> to a signed integer sample with 24bit range.
     * <p>
     * For little endian, buffer[byteOffset] is interpreted as lowest byte,
     * whereas it is interpreted as highest byte in big endian.
     * <p> This is a reference function.
     */ 
    public static int bytesToInt24( byte [] buffer, int byteOffset, 
                                    boolean isBigEndian) 
    { 
        return isBigEndian?
            ((buffer[byteOffset]<<16) // let Java handle sign-bit 
             | ((buffer[byteOffset+1] & 0xFF)<<8) // inhibit sign-bit handling 
             | (buffer[byteOffset+2] & 0xFF)):
            
            ((buffer[byteOffset+2]<<16) // let Java handle sign-bit 
              | ((buffer[byteOffset+1] & 0xFF)<<8) // inhibit sign-bit handling 
              | (buffer[byteOffset] & 0xFF));
    } 

    /**
     * Converts a 4 successive bytes starting at <code>byteOffset</code> in 
     * <code>buffer</code> to a signed 32bit integer sample.
     * <p>
     * For little endian, buffer[byteOffset] is interpreted as lowest byte,
     * whereas it is interpreted as highest byte in big endian.
     * <p> This is a reference function.
     */ 
    public static int bytesToInt32( byte [] buffer, int byteOffset, 
                                    boolean isBigEndian) 
    { 
        return isBigEndian?
            ((buffer[byteOffset]<<24) // let Java handle sign-bit 
             | ((buffer[byteOffset+1] & 0xFF)<<16) // inhibit sign-bit handling 
             | ((buffer[byteOffset+2] & 0xFF)<<8) // inhibit sign-bit handling 
             | (buffer[byteOffset+3] & 0xFF)):
        
            ((buffer[byteOffset+3]<<24) // let Java handle sign-bit 
             | ((buffer[byteOffset+2] & 0xFF)<<16) // inhibit sign-bit handling 
             | ((buffer[byteOffset+1] & 0xFF)<<8) // inhibit sign-bit handling 
             | (buffer[byteOffset] & 0xFF));
    } 
}//end class WaveScanner
