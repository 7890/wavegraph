//tb/150121

package ch.lowres.wavegraph;

import java.io.*;

//=======================================================
public abstract class AudioFileParser 
{
	protected AudioFormat af;
	protected boolean printDebug=false;

	protected FileInputStream fis;
	protected DataInputStream dis;

//=======================================================
	public abstract AudioFormat parse(String file);

//=======================================================
	/**
	 * read_ieee_extended
	 * Extended precision IEEE floating-point conversion routine.
	 * @argument DataInputStream
	 * @return double
	 * @exception IOException
	 */
	public static double read_ieee_extended(DataInputStream dis) throws IOException
	{
		double f=0;
		int expon=0;
		long hiMant=0, loMant=0;
		long t1, t2;
		double HUGE=((double)3.40282346638528860e+38);

		expon=dis.readUnsignedShort();

		t1=(long)dis.readUnsignedShort();
		t2=(long)dis.readUnsignedShort();
		hiMant=t1 << 16 | t2;

		t1=(long)dis.readUnsignedShort();
		t2=(long)dis.readUnsignedShort();
		loMant=t1 << 16 | t2;

		if(expon==0 && hiMant==0 && loMant==0)
		{
			f=0;
		}
		else
		{
			if (expon==0x7FFF)
			{
				f=HUGE;
			}
			else
			{
				expon-=16383;
				expon-=31;
				f=(hiMant * Math.pow(2, expon));
				expon-=32;
				f+=(loMant * Math.pow(2, expon));
			}
		}

		return f;
	}//end read_ieee_extended

//also in AudioFormat - need to consolidate
//=======================================================
	//http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes)
	{
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ )
		{
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

//=======================================================
	//http://stackoverflow.com/questions/1507780/searching-for-a-sequence-of-bytes-in-a-binary-file-with-java
	public static int bytesIndexOf(byte[] Source, byte[] Search, int fromIndex)
	{
		boolean Find = false;
		int i;
		for (i = fromIndex;i<=Source.length-Search.length;i++)
		{
			if(Source[i]==Search[0])
			{
				Find = true;
				for (int j = 0;j<Search.length;j++)
				{
					if (Source[i+j]!=Search[j])
					{
						Find = false;
					}
				}
			}
			if(Find)
			{
				break;
			}
		}
		if(!Find)
		{
			return -1;
		}
		return i;
	}//end bytesIndexOf

//=======================================================
	public static void p(String s)
	{
		System.out.println(s);
	}
}//end class AudioFileParser
