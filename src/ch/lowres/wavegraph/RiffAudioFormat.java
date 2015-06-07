//tb/150121

package ch.lowres.wavegraph;

import java.io.*;
import java.util.*;
import java.math.*;

import java.text.DecimalFormat;

//=======================================================
public class RiffAudioFormat extends AudioFormat
{
	//http://www-mmsp.ece.mcgill.ca/documents/AudioFormats/WAVE/WAVE.html

	public static final String[] infotype={"IARL", "IART", "ICMS", "ICMT", "ICOP", "ICRD", "ICRP", "IDIM",
		"IDPI", "IENG", "IGNR", "IKEY", "ILGT", "IMED", "INAM", "IPLT", "IPRD", "ISBJ",
		"ISFT", "ISHP", "ISRC", "ISRF", "ITCH", "ISMP", "IDIT" };

	public static final String[] infodesc={"Archival location", "Artist", "Commissioned", "Comments", "Copyright", 
		"Creation date","Cropped", "Dimensions", "Dots per inch", "Engineer", "Genre", "Keywords", 
		"Lightness settings", "Medium", "Name of subject", "Palette settings", "Product", "Description",
		"Software package", "Sharpness", "Source", "Source form", "Digitizing technician", 
		"SMPTE time code", "Digitization time"};

	protected static Hashtable listinfo=null;

	//byte sequence found after subtype in extensible waves
	public final static byte[] tail=new byte[14];

//=======================================================
	public RiffAudioFormat()
	{
		minimalHeaderSize=44;

		listinfo=new Hashtable();
		for(int i=0;i<infotype.length;i++)//build the hashtable of values for easy searching
		{
			listinfo.put(infotype[i], infodesc[i]);
		}
/*
http://www-mmsp.ece.mcgill.ca/documents/AudioFormats/WAVE/WAVE.html
The first two bytes of the GUID form the sub-code specifying the data format code, e.g. WAVE_FORMAT_LINEAR_PCM. 

The remaining 14 bytes contain a fixed string, "
\x00\x00\x00\x00
\x10\x00\x80\x00
\x00\xAA\x00\x38

\x9B\x71".
*/
		tail[0]=(byte)0x00;
		tail[1]=(byte)0x00;
		tail[2]=(byte)0x00;
		tail[3]=(byte)0x00;

		tail[4]=(byte)0x10;
		tail[5]=(byte)0x00;
		tail[6]=(byte)0x80;
		tail[7]=(byte)0x00;

		tail[8]=(byte)0x00;
		tail[9]=(byte)0xAA;
		tail[10]=(byte)0x00;
		tail[11]=(byte)0x38;

		tail[12]=(byte)0x9B;
		tail[13]=(byte)0x71;
	}//end constructor
}//end class RiffAudioFormat
