//tb/150117

package ch.lowres.wavegraph;

//big and little endian
//http://www.cs.umd.edu/class/sum2003/cmsc311/Notes/Data/endian.html

//=======================================================
class ToFloat
{
	private static LawEncoding law;

//2 bytes
//MIN_VALUE     -2^15
//MAX_VALUE -1 + 2^15
	final static long m16_div=32767; //-1+ (2^16)/2 == -1+2^15

//3 bytes
//MIN_VALUE     -2^23
//MAX_VALUE -1 + 2^23
	final static long m24_div=8388607; //-1+ (2^24)/2 == -1+2^23

//4 bytes
//MIN_VALUE     -2^31
//MAX_VALUE -1 + 2^31
	final static long m32_div=2147483647L; //-1+ (2^32)/2 == -1+2^31

//8 bytes
//MIN_VALUE     -2^63
//MAX_VALUE -1 + 2^63
	final static long m64_div=9223372036854775807L; //-1+ (2^64)/2 == -1+2^63

/*
//from http://www-mmsp.ece.mcgill.ca/Documents/Software/Packages/libtsp/AF/AFopnRead.html
//note the range [-1, +1)

For the fixed point file data representations, 
read operations return data values as follows. 
The scaling factor shown below is applied to the data 
in the file to give an output in the range [-1, +1).

   data format     file data values              scaling factor
  8-bit mu-law    [-32,124, +32,124]              1/32768
  8-bit A-law     [-32,256, +32,256]              1/32768
  8-bit integer   [-128, +127]                    1/256
  16-bit integer  [-32,768, +32,767]              1/32768
  24-bit integer  [-8,388,608, +8,388,607]        1/8388608
  32-bit integer  [-2,147,483,648, 2,147,483,647] 1/2147483648
*/

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
	public static float signedByte(byte b1)
	{
		return (float)b1/255;
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
		return (float) bytesToInt32(sample,0,isBigEndian) / m32_div;
	}

//=======================================================
///untested
	public static float signed64(byte[] sample, boolean isBigEndian)
	{
		return (float) bytesToInt64(sample,0,isBigEndian) / m64_div;
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

//=======================================================
///untested
	public static long bytesToInt64( byte [] buffer, int byteOffset, boolean isBigEndian) 
	{
		if(isBigEndian)
		{
			long value=buffer[byteOffset+7];
			value|=((long)(buffer[byteOffset+6]&0xFF)<<8);
			value|=((long)(buffer[byteOffset+5]&0xFF)<<16);
			value|=((long)(buffer[byteOffset+4]&0xFF)<<24);
			value|=((long)(buffer[byteOffset+3]&0xFF)<<32);
			value|=((long)(buffer[byteOffset+2]&0xFF)<<40);
			value|=((long)(buffer[byteOffset+1]&0xFF)<<48);
			value|=((long)(buffer[byteOffset]&0xFF)<<56);
			return value;
		}
		else
		{
			long value=buffer[byteOffset];
			value|=((long)(buffer[byteOffset+1]&0xFF)<<8);
			value|=((long)(buffer[byteOffset+2]&0xFF)<<16);
			value|=((long)(buffer[byteOffset+3]&0xFF)<<24);
			value|=((long)(buffer[byteOffset+4]&0xFF)<<32);
			value|=((long)(buffer[byteOffset+5]&0xFF)<<40);
			value|=((long)(buffer[byteOffset+6]&0xFF)<<48);
			value|=((long)(buffer[byteOffset+7]&0xFF)<<56);
			return value;
		}
	}
}//end class WaveScanner
