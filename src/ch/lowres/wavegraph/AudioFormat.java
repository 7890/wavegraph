//tb/150121

package ch.lowres.wavegraph;

import java.io.*;
import java.util.*;
import java.math.*;

import java.text.DecimalFormat;

//PCM audio
//=======================================================
public class AudioFormat 
{
	static final int RIFF_FILE=1;
	static final int AIFC_FILE=2;
	static final int AIFF_FILE=3;

	protected int fileType=0;

	protected String selectedFile="";

	protected int minimalHeaderSize=0;

	protected long fileSize=0; // total file size (bytes)
	protected long dataSize=0; // size of useable sample byte data
	protected long dataOffset=0; //absolute byte where sample data starts

	static final int WAVE_FORMAT_LINEAR_PCM=0x0001;
	static final int WAVE_FORMAT_IEEE_FLOAT=0x0003;
	static final int WAVE_FORMAT_ALAW=0x0006;
	static final int WAVE_FORMAT_ULAW=0x0007;
	static final int WAVE_FORMAT_EXTENSIBLE=0xFFFE;

	protected boolean isPCM=false;

	protected int wFormatTag=0;
	protected int wSubFormat=0; //if extensible

	protected int nChannels=0;
	protected int nSamplesPerSec=0;
	protected int nAvgBytesPerSec=0;
	protected int nBlockAlign=0;
	protected int nBitsPerSample=0;

	protected boolean isValid=false;

//=======================================================
	public String getWaveFormat()
	{
		StringBuffer sb=new StringBuffer();

		if(wFormatTag==WAVE_FORMAT_LINEAR_PCM)
		{
			sb.append("LINEAR PCM");
		}

		else if(wFormatTag==WAVE_FORMAT_EXTENSIBLE)
		{
			sb.append("EXTENSIBLE ");

			if(wSubFormat==WAVE_FORMAT_LINEAR_PCM)
			{
				sb.append("LINEAR PCM");
			}
			else if(wSubFormat==WAVE_FORMAT_IEEE_FLOAT)
			{
				sb.append("IEEE FLOAT");
			}
			else if(wSubFormat==WAVE_FORMAT_ALAW)
			{
				sb.append("A-LAW PCM");
			}
			else if(wSubFormat==WAVE_FORMAT_ULAW)
			{
				sb.append("U-LAW PCM");
			}
			else
			{
				sb.append("UNKNOWN");
			}
		}

		else if(wFormatTag==WAVE_FORMAT_IEEE_FLOAT)
		{
			sb.append("IEEE FLOAT");
		}
		else if(wFormatTag==WAVE_FORMAT_ALAW)
		{
			sb.append("A-LAW PCM");
		}
		else if(wFormatTag==WAVE_FORMAT_ULAW)
		{
			sb.append("U-LAW PCM");
		}

		else
		{
			sb.append("UNKNOWN");
		}

		return sb.toString();
	}//end getWaveFormat

//=======================================================
	public static AudioFileParser createParser(String file)
	{
		byte[] bytes=null;
		try 
		{
			bytes=new byte[16];
			FileInputStream fis=new FileInputStream(file);
			DataInputStream dis=new DataInputStream(fis); 

			//try to read 16 bytes
			dis.read(bytes);
			dis.close();
			fis.close();

			if(bytes!=null)
			{
				//"extended" header dump
				//System.out.print("header dump "+bytes.length+" bytes: 0x"+bytesToHex(bytes)+"  |  ");
				for(int i=0;i<bytes.length;i++)
				{
					System.out.print((char)bytes[i]);
				}
				p("");

//00000000  52 49 46 46 48 c6 60 03  57 41 56 45 66 6d 74 20  |RIFFH.`.WAVEfmt |
				if(bytes[0]=='R'
				&& bytes[1]=='I'
				&& bytes[2]=='F'
				&& bytes[3]=='F')
				{
					p("using RiffAudioFileParser");
					return new RiffAudioFileParser();
				}
				else
				{
//00000000  46 4f 52 4d 00 01 6f 94  41 49 46 46 43 4f 4d 4d  |FORM..o.AIFFCOMM|
//00000000  46 4f 52 4d 00 00 07 5a  41 49 46 43 46 56 45 52  |FORM...ZAIFCFVER|

//test further...
//only alternative atm
					p("using AiffAudioFileParser");
					return new AiffAudioFileParser();
				}
			}
		}
		catch(Exception e){e.printStackTrace();}

		//dummy / !isValid
		return new RiffAudioFileParser();
	}//end createParser

//=======================================================
	public void print()
	{
		p("==================================");
		p("file: "                      +getFileName());//selectFile);
		p("file size: "                 +getFileSize()+" bytes");
		//p("== fmt chunk ==");
		p("format: "                    +getWaveFormat());
	        p("channels: "                  +getChannels()+" "+(getChannels()==1 ? "(mono)" : ""));
		p("samples per sec: "           +getSampleRate());

		p("bytes per sec: "             +getAverageBytesPerSecond());

		p("frame size (block align): "  +getBlockAlign()+" bytes");
		p("bits per sample: "           +getBitsPerSample());
		//p("== data chunk ==");
		p("total sample data size: "    +getDataSize()+" bytes");
		p("sample data starts at byte: "+getDataOffset());
		p("frames: "                    +getFrameCount());
		p("duration: "                  +getDurationString());
		p("==================================");
	}

//=======================================================
	public boolean isValid()
	{
		return isValid;
	}

//=======================================================
	public String getFileName()
	{
		return selectedFile;
	}

//=======================================================
	public int getFileType()
	{
		return fileType;
	}

//=======================================================
	public String getFileTypeName()
	{
		if(fileType==RIFF_FILE)
		{
			return "RIFF";
		}
		else if(fileType==AIFC_FILE)
		{
			return "AIFC";
		}
		else if(fileType==AIFF_FILE)
		{
			return "AIFF";
		}
		else
		{
			return "UNKNOWN";
		}
	}

//=======================================================
	public long getFileSize()
	{
		return fileSize;
	}

//=======================================================
	public long getDataSize()
	{
		return dataSize;
	}

//=======================================================
	public long getDataOffset()
	{
		return dataOffset;
	}

//=======================================================
	public int getSampleRate()
	{
		return nSamplesPerSec;
	}

//=======================================================
	public int getAverageBytesPerSecond()
	{
		return nAvgBytesPerSec;
	}

//=======================================================
	public int getChannels()
	{
		return nChannels;
	}

//=======================================================
	public int getBitsPerSample()
	{
		return nBitsPerSample;
	}

//=======================================================
	public int getBlockAlign()
	{
		return nBlockAlign;
	}

//=======================================================
	//every channel consists of the same n samples
	public int getFrameCount()
	{
		return (int)(dataSize/nBlockAlign);
	}

//=======================================================
	public int getTag()
	{
		return wFormatTag;
	}

//=======================================================
	public int getSubFormat()
	{
		return wSubFormat;
	}

//=======================================================
	public boolean isPCM()
	{
		return isPCM;
	}

//=======================================================
	public String getDurationString()
	{
		return getDurationString(getFrameCount(),getSampleRate());
	}

//=======================================================
	public String getDurationString(long frames)
	{
		return getDurationString(frames,getSampleRate());
	}

//=======================================================
	public static String getDurationString(long frames, int sampleRate )
	{
		if(frames<=0 || sampleRate<=0)
		{
			//return "00:00:00.000=";
			return "      00.000=";
		}
		//for uncompressed, temporal linear data

		DecimalFormat df=new DecimalFormat("#00");
		DecimalFormat dfSec=new DecimalFormat("#00.000");
		//dfSec.setRoundingMode(RoundingMode.HALF_UP);
		dfSec.setRoundingMode(RoundingMode.HALF_DOWN);

		long hours=0;
		long minutes=0;
		double seconds=0;

		long secondsFloor=0;
		double secondsFraction=0;

		double modDelta=0;
		boolean roundDown=false;

		long waveduration=1000*frames/sampleRate;

		hours=waveduration/3600000L;
		minutes=(waveduration/60000) % 60;
		//seconds=0.001*(waveduration % 60000);//double secs.

		//check if will be rounded up or down (or ~ equal)
		secondsFloor=(long)Math.floor((double)frames/sampleRate);
		secondsFraction=((double)frames/sampleRate)-secondsFloor;

		modDelta=secondsFraction % 0.001;
		roundDown=(modDelta>0.0005 ? false : true);

		seconds=secondsFraction+(secondsFloor % 60);

		String returnString="";
		if(hours>0)
		{
			returnString+=df.format(hours)+":";
		}
		else
		{
			returnString+="   ";
		}
		if(minutes>0)
		{
			returnString+=df.format(minutes)+":";
		}
		else if(hours>0) //&& minutes 0
		{
			returnString+="00:";
		}
		else
		{
			returnString+="   ";
		}

		//if delta is small, indicate with "="
		if(modDelta<0.0000001 || modDelta > 0.0009999)
		{
			return returnString+=dfSec.format(seconds)+"=";
		}
		else
		{
			//"^": value was rounded up
			//"_": value was rounded down
			return returnString+=dfSec.format(seconds)+(roundDown ? "_" : "^");
		}
	}//end getDurationString

//=======================================================
	public static void p(String s)
	{
		System.out.println(s);
	}
}//end class AudioFormat
