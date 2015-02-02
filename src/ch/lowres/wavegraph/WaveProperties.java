//tb/150121
/*
adapted from original file
http://www.jensign.com/riffparse/RiffRead32.txt / Michel I. Gallant 
Copyright JavaScience Consulting 01/03/2003, 10/26/2006 , 11/15/2006, 03/31/2007, 05/17/2007
*/

//http://www-mmsp.ece.mcgill.ca/documents/AudioFormats/WAVE/WAVE.html

package ch.lowres.wavegraph;

import java.io.*;
import java.awt.*;
import java.util.Date;
import java.util.Hashtable;

import java.text.DecimalFormat;

//=======================================================
public class WaveProperties 
{
	static final int WAVE_FORMAT_LINEAR_PCM=0x0001;
	static final int WAVE_FORMAT_IEEE_FLOAT=0x0003;
	static final int WAVE_FORMAT_ALAW=0x0006;
	static final int WAVE_FORMAT_ULAW=0x0007;
	static final int WAVE_FORMAT_EXTENSIBLE=0xFFFE;

	static final String title="RiffRead32 ";
	static final String[] infotype={"IARL", "IART", "ICMS", "ICMT", "ICOP", "ICRD", "ICRP", "IDIM",
		"IDPI", "IENG", "IGNR", "IKEY", "ILGT", "IMED", "INAM", "IPLT", "IPRD", "ISBJ",
		"ISFT", "ISHP", "ISRC", "ISRF", "ITCH", "ISMP", "IDIT" };

	static final String[] infodesc={"Archival location", "Artist", "Commissioned", "Comments", "Copyright", 
		"Creation date","Cropped", "Dimensions", "Dots per inch", "Engineer", "Genre", "Keywords", 
		"Lightness settings", "Medium", "Name of subject", "Palette settings", "Product", "Description",
		"Software package", "Sharpness", "Source", "Source form", "Digitizing technician", 
		"SMPTE time code", "Digitization time"};

	static Hashtable listinfo=null;

	//byte sequence found after subtype in extensible waves
	static byte[] tail=new byte[14];

	private String selectFile="";

	private long filesize=0; // total file size
	private int riffdata=0;// size of RIFF data chunk.
	private long datasize=0; // size of DATA chunk

	private boolean isPCM=false;

	private int wFormatTag=0;
	private int wSubFormat=0; //if extensible

	private int nChannels=0;
	private int nSamplesPerSec=0;
	private int nAvgBytesPerSec=0;
	private int nBlockAlign=0;
	private int nBitsPerSample=0;

	private String waveFormat="N/A";

	private int minimalHeaderSize=44;
	private boolean isValid=false;

//=======================================================
	public WaveProperties()
	{
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


//=======================================================
	public void reset()
	{
		//reset values
		filesize=0; // total file size
		riffdata=0;// size of RIFF data chunk.
		datasize=0; // size of DATA chunk

		isPCM=false;

		wFormatTag=0;
		wSubFormat=0;
		nChannels=0;
		nSamplesPerSec=0;
		nAvgBytesPerSec=0;
		nBlockAlign=0;
		nBitsPerSample=0;

		waveFormat="N/A";
		isValid=false;

		selectFile="";
	}

//=======================================================
	public boolean read(String file)
	{
		//reset values
		reset();
		selectFile=file;

		StringBuffer txtbuf=new StringBuffer();
		FileInputStream fis=null;
		DataInputStream dis=null;

		int byteread=0;

		int chunkSize=0, infochunksize=0, bytecount=0, listbytecount=0;
		String sfield="", infofield="", infodescription="", infodata="";

		try 
		{
			fis=new FileInputStream(selectFile);
			dis=new DataInputStream(fis); 

			riffdata=0;// size of RIFF data chunk.
			chunkSize=0;
			infochunksize=0;
			bytecount=0;
			listbytecount=0;
			sfield="";
			infofield="";
			infodescription="";
			infodata="";
			String sp="   ";// spacer string.
			String indent=sp+"   ";
			filesize=(new File(selectFile)).length();// get file length.

			txtbuf.append(selectFile+"    LENGTH:  "+filesize+" bytes\n");

			if(filesize<minimalHeaderSize)
			{
				p("file too small: only "+filesize+" bytes! possibly no valid RIFF WAVE header or RAW.");
				return isValid;
			}

			/*--------  Get RIFF chunk header ---------*/
			for(int i=1;i<=4;i++)
			{
				sfield+=(char)dis.readByte();
			}
			if(!sfield.equals("RIFF"))
			{
				txtbuf.append(" **** Not a valid RIFF file ****\n");
				p("file doesn't start with magic RIFF byte sequence: got "+sfield);
				return isValid;
			}

			for(int i=0;i<4;i++)
			{
				chunkSize += dis.readUnsignedByte()*(int)Math.pow(256,i);
			}
			txtbuf.append("\n"+sfield+" ----- data size: "+chunkSize+ " bytes\n");

			sfield="";
			for(int i=1;i<=4;i++)
			{
				sfield+=(char)dis.readByte();
			}
			txtbuf.append("Form-type: "+ sfield+"\n");

			riffdata=chunkSize;

			bytecount=4;// initialize bytecount to include RIFF form-type bytes.

			while (bytecount < riffdata)
			{// check for chunks inside RIFF data area. 
				sfield="";
				int firstbyte=dis.readByte();
				if(firstbyte==0)
				{//if previous data had odd bytecount, was padded by null so skip
					bytecount++;
					continue;
				}
				sfield+=(char)firstbyte;//if we have a new chunk
				for(int i=1;i<=3;i++)
				{
					sfield+=(char)dis.readByte();
				}
				chunkSize=0;
				for(int i=0;i<4;i++)
				{
					chunkSize += dis.readUnsignedByte()*(int)Math.pow(256,i);
				}
				bytecount += (8+chunkSize);
				txtbuf.append("\n"+sfield+" ----- data size: "+chunkSize+ " bytes\n");

				if(sfield.equals("data"))//get data size to compute duration later.
				{
					datasize=chunkSize;
				}
				if(sfield.equals("fmt "))
				{// extract info from "format" chunk.
					if(chunkSize<16)
					{
						txtbuf.append("**** Not a valid fmt chunk ****\n");
						p("file has invalid valid fmt chunk");
						return isValid;
					}
					wFormatTag=dis.readUnsignedByte()+ dis.readUnsignedByte()*256;
					if(wFormatTag==WAVE_FORMAT_LINEAR_PCM 
					|| wFormatTag==WAVE_FORMAT_EXTENSIBLE 
					|| wFormatTag==WAVE_FORMAT_IEEE_FLOAT
					|| wFormatTag==WAVE_FORMAT_ULAW
					|| wFormatTag==WAVE_FORMAT_ALAW
					)
					{
						isPCM=true;
					}
					if(wFormatTag==WAVE_FORMAT_LINEAR_PCM)
					{
						txtbuf.append(indent+"wFormatTag: WAVE_FORMAT_LINEAR_PCM\n");
						waveFormat="LINEAR PCM";
					}
					else if(wFormatTag==WAVE_FORMAT_EXTENSIBLE)
					{
						txtbuf.append(indent+"wFormatTag: WAVE_FORMAT_EXTENSIBLE\n");
						waveFormat="EXTENSIBLE";
					}
					else if(wFormatTag==WAVE_FORMAT_IEEE_FLOAT)
					{
						txtbuf.append(indent+"wFormatTag: WAVE_FORMAT_IEEE_FLOAT\n");
						waveFormat="IEEE FLOAT";
					}
					else if(wFormatTag==WAVE_FORMAT_ALAW)
					{
						txtbuf.append(indent+"wFormatTag: WAVE_FORMAT_ALAW\n");
						waveFormat="A-LAW PCM";
					}
					else if(wFormatTag==WAVE_FORMAT_ULAW)
					{
						txtbuf.append(indent+"wFormatTag: WAVE_FORMAT_ULAW\n");
						waveFormat="U-LAW PCM";
					}
					else
					{
						txtbuf.append(indent+"wFormatTag: unknown format "+wFormatTag+"\n");
						waveFormat="UNKNOWN";//NON-PCM";
					}
					nChannels=dis.readUnsignedByte();
					dis.skipBytes(1);
					txtbuf.append(indent+"nChannels: "+nChannels+"\n");
					nSamplesPerSec=0;
					for(int i=0;i<4;i++)
					{ 
						nSamplesPerSec += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					txtbuf.append(indent+"nSamplesPerSec: "+nSamplesPerSec+"\n");
					nAvgBytesPerSec=0;
					for(int i=0;i<4;i++)
					{
						nAvgBytesPerSec += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					txtbuf.append(indent+"nAvgBytesPerSec: "+nAvgBytesPerSec+"\n");
					nBlockAlign=0;
					for(int i=0;i<2;i++)
					{
						nBlockAlign += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					txtbuf.append(indent+"nBlockAlign: "+nBlockAlign+"\n");
					if(isPCM)
					{// if PCM or EXTENSIBLE format
						nBitsPerSample=dis.readUnsignedByte();
						dis.skipBytes(1);
						txtbuf.append(indent+"nBitsPerSample: "+nBitsPerSample+"\n");
					}
					else
					{
						dis.skipBytes(2);
					}
					dis.skipBytes(chunkSize-16);//skip over any extra bytes in format specific field.
				}//end sfield.equals("fmt ")
				else if(sfield.equals("LIST"))
				{
					String listtype="";
					for(int i=1;i<=4;i++)
					{ 
						listtype+=(char)dis.readByte();
					}
					//if(! ///skip over all the way, prevent any oddities
					if(listtype.equals("INFO"))
					{ //skip over LIST chunks which don't contain INFO subchunks
						dis.skipBytes(chunkSize-4);
						continue;
					}

					listbytecount=4;
					txtbuf.append("\n------- INFO chunks -------\n");

					while(listbytecount < chunkSize)
					{//iterate over all entries in LIST chunk
						infofield="";
						infodescription="";
						infodata="";

						firstbyte=dis.readByte();
						if(firstbyte==0)
						{//if previous data had odd bytecount, was padded by null so skip
							listbytecount++;
							continue;
						}
						infofield+=(char)firstbyte;//if we have a new chunk
						for(int i=1;i<=3;i++)//get the remaining part of info chunk name ID
						{
							infofield+=(char)dis.readByte();
						}
						infochunksize=0;
						for(int i=0;i<4;i++)//get the info chunk data byte size
						{
							infochunksize += dis.readUnsignedByte()*(int)Math.pow(256,i);
						}
						listbytecount += (8+infochunksize);

						for(int i=0;i<infochunksize;i++)
						{//get the info chunk data
							byteread=dis.readByte();
							if(byteread==0)//if null byte in string, ignore it
							{
								continue;
							}
							infodata+=(char)byteread;
						}

						infodescription=(String)listinfo.get(infofield);
						if(infodescription !=null)
						{
							txtbuf.append(infodescription+": "+infodata+"\n");
						}
						else
						{
							txtbuf.append("unknown : "+infodata+"\n");
						}
					}//end (listbytecount < chunkSize)
					//------- end iteration over LIST chunk ------------
					txtbuf.append("------- end INFO chunks -------\n");
				}//end sfield.equals("LIST")
				else// if NOT the fmt or LIST chunks just skip over the data.
				{
					dis.skipBytes(chunkSize);
				}
			}// end while (bytecount < riffdata)
			//----------- End of chunk iteration -------------

			if(isPCM && datasize>0)
			{//compute duration of PCM wave file
				long waveduration=1000L*datasize/nAvgBytesPerSec;//in msec units
				long mins=waveduration/ 60000;	// integer minutes
				double secs=0.001*(waveduration % 60000);	//double secs.
				txtbuf.append("\nwav duration: "+mins+" mins "+secs+ " sec\n");
			}

			txtbuf.append("\nFinal RIFF data bytecount: "+bytecount+"\n");

			if((8+bytecount)!= (int)filesize)
			{
				txtbuf.append("!!!!!!! Problem with file structure !!!!!!!!!\n");
				p("file size seems odd");
			}
			else 
			{
				txtbuf.append("File chunk structure consistent with valid RIFF\n");
				isValid=true;
			}
			//p(txtbuf.toString());

			//*************
			//try find out subtype if float or int
			if(wFormatTag==WAVE_FORMAT_EXTENSIBLE)
			{
				byte[] bytes=null;
				try 
				{
					//read all that's not sample data at the beginning of a file and put to buffer
					bytes=new byte[getDataOffset()];

					FileInputStream fis2=new FileInputStream(selectFile);
					DataInputStream dis2=new DataInputStream(fis2); 

					dis2.read(bytes);
					dis2.close();
					fis2.close();
				}
				catch(Exception e){e.printStackTrace();}

				if(bytes!=null)
				{
					//"extended" header dump
					p("extended header: total "+bytes.length+" bytes: "+bytesToHex(bytes));
					p("looking for byte sequence: "+bytesToHex(tail));

					int found=bytesIndexOf(bytes, tail, 0);
					if(found!=-1)
					{
						p("found: "+bytes[found-2]+" "+bytes[found-1]);

/*
   -2  -1  pos from found
seq 1  0 -> dec. 1
seq 3  0 -> dec. 3  //float
others not of interest
*/
						if(bytes[found-2]==1 && bytes[found-1]==0)
						{
							wSubFormat=WAVE_FORMAT_LINEAR_PCM;
						}
						else if(bytes[found-2]==3 && bytes[found-1]==0)
						{
							wSubFormat=WAVE_FORMAT_IEEE_FLOAT;
						}
					}
				}
				else
				{
					p("/!\\ subtype not found");
				}
			}//end WAVE_FORMAT_EXTENSIBLE
		}// end global try

		catch(Exception e){e.printStackTrace();}
		finally 
		{
			try
			{
				dis.close();// close all streams.
				fis.close();
			}
			catch(Exception e){}
		}

		return isValid;
	}//end read

//=======================================================
	public boolean isValid()
	{
		return isValid;
	}

//=======================================================
	public String getFileName()
	{
		return selectFile;
	}

//=======================================================
	public long getFileSize()
	{
		return filesize;
	}

//=======================================================
	public long getDataSize()
	{
		return datasize;
	}

//=======================================================
	public int getDataOffset()
	{
		return (int)(filesize-datasize);
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
		return (int)(datasize/nBlockAlign);
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
	public String getWaveFormat()
	{
		//eventually add subformat
		if(wSubFormat==WAVE_FORMAT_LINEAR_PCM)
		{
			return waveFormat+" LINEAR PCM";
		}
		else if(wSubFormat==WAVE_FORMAT_IEEE_FLOAT)
		{
			return waveFormat+" IEEE FLOAT";
		}
		//multichanel a-law, u-law?

		return waveFormat;
	}

//=======================================================
	public String getDurationString()
	{
		//return getDurationString(datasize,nAvgBytesPerSec);
		return getDurationString(getFrameCount(),getSampleRate());
	}

//=======================================================
	public String getDurationString(long frames)
	{
		//return getDurationString(datasize,nAvgBytesPerSec);
		return getDurationString(frames,getSampleRate());
	}

//=======================================================
//	public static St<ring getDurationString(long datasize_, int avgbps_)
	public static String getDurationString(long frames, int sampleRate )
	{
		if(frames<=0 || sampleRate<=0)
		{
			return "00:00:00.000";
		}
		//for uncompressed, temporal linear data

		DecimalFormat df=new DecimalFormat("#00");
		DecimalFormat dfSec=new DecimalFormat("#00.000");

		long hours=0;
		long minutes=0;
		double seconds=0;

		long waveduration=1000*frames/sampleRate;

//		if(datasize_>0)
		{//compute duration of PCM wave file
//			long waveduration=1000*datasize_/avgbps_;//datasize/nAvgBytesPerSec;//in msec units
			hours=waveduration/3600000L;
			minutes=(waveduration/60000) % 60;
			seconds=0.001*(waveduration % 60000);       //double secs.
		}
		return df.format(hours)+":"+df.format(minutes)+":"+dfSec.format(seconds);
	}

//=======================================================
	public void print()
	{
		p("==================================");
		p("file: "			+getFileName());//selectFile);
		p("file size: "			+getFileSize()+" bytes");
		//p("== fmt chunk ==");
		p("format: "			+getWaveFormat());
		p("channels: "			+getChannels()+" "+(getChannels()==1 ? "(mono)" : ""));
		p("samples per sec: "		+getSampleRate());

		p("bytes per sec: "		+getAverageBytesPerSecond());

		p("frame size (block align): "  +getBlockAlign()+" bytes");
		p("bits per sample: "		+getBitsPerSample());
		//p("== data chunk ==");
		p("total sample data size: "	+getDataSize()+" bytes");
		p("sample data starts at byte: "+getDataOffset());
		p("frames: "			+getFrameCount());
		p("duration: "			+getDurationString());
		p("==================================");
	}

//=======================================================
	public static void p(String s)
	{
		System.out.println(s);
	}

//=======================================================
	//http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
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
	private int bytesIndexOf(byte[] Source, byte[] Search, int fromIndex)
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
}//end class WaveProperties
