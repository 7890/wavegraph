//tb/150315

package ch.lowres.wavegraph;

import java.io.*;
import java.util.*;
import java.math.*;

import java.text.DecimalFormat;

//=======================================================
public class RiffAudioFileParser extends AudioFileParser
{
	long currentOffset=0;
	long lastStartMark=0;

	String indent="      ";
	StringBuffer txtbuf;

//=======================================================
	public AudioFormat parse(String file)
	{
		//audio format initialize (isValid==false)
		af = new RiffAudioFormat();
		af.selectedFile=file;

		try 
		{
			fis=new FileInputStream(af.selectedFile);
			dis=new DataInputStream(fis); 

			txtbuf=new StringBuffer();

			int chunkSize=0;

			currentOffset=0;
			lastStartMark=0;

			String sfield="";
			String infofield="";
			String infodescription="";
			String infodata="";

			af.fileSize=(new File(af.selectedFile)).length();// get total file size (bytes)

			txtbuf.append(af.selectedFile+"\n");
			txtbuf.append("LENGTH: "+af.fileSize+" bytes\n");


			if(af.fileSize<af.minimalHeaderSize)
			{
				p("file too small: only "+af.fileSize+" bytes! possibly no valid RIFF WAVE header or RAW.");
				//
			}

			//--------  Get RIFF chunk header ---------
			for(int i=0;i<4;i++)
			{
				sfield+=(char)dis.readByte();
			}
			currentOffset+=4;
			if(!sfield.equals("RIFF"))
			{
				txtbuf.append(" **** Not a valid RIFF file ****\n");
				p("file doesn't start with magic RIFF byte sequence: got "+sfield);
				//
			}

			af.fileType=af.RIFF_FILE;

			for(int i=0;i<4;i++)
			{
				chunkSize += dis.readUnsignedByte()*(int)Math.pow(256,i);
			}
			currentOffset+=4;
			txtbuf.append("\n"+sfield+" at "+lastStartMark+", data start "+currentOffset+", data size "+chunkSize+ " bytes\n");

			lastStartMark=currentOffset;

			sfield="";
			for(int i=0;i<4;i++)
			{
				sfield+=(char)dis.readByte();
			}
			currentOffset+=4;
			txtbuf.append("Form-type: "+sfield+" at "+lastStartMark+", data start "+currentOffset+"\n"); 

			int riffData=chunkSize;

			if(riffData+8>af.fileSize)
			{
				txtbuf.append("!!!! riff data size to large !!!!\n"); 
				riffData=(int)(af.fileSize-currentOffset-8);
				txtbuf.append("limiting riff data size to "+riffData+"\n"); 
			}

			int bytecount=4;// initialize bytecount to include RIFF form-type bytes.

			while (bytecount < riffData)
			{// check for chunks inside RIFF data area. 

				lastStartMark=currentOffset;
				sfield="";
				int firstbyte=dis.readByte();
				currentOffset++;
				if(firstbyte==0)
				{//if previous data had odd bytecount, was padded by null so skip
					txtbuf.append("odd bytecount padded at "+(currentOffset-1)+" (+1)\n");
					bytecount++;
					continue;
				}
				sfield+=(char)firstbyte;//if we have a new chunk read next 3 bytes
				for(int i=0;i<3;i++)
				{
					sfield+=(char)dis.readByte();
				}
				currentOffset+=3;

				chunkSize=0;
				for(int i=0;i<4;i++)
				{
					chunkSize += dis.readUnsignedByte()*(int)Math.pow(256,i);
				}
				bytecount += (8+chunkSize);
				currentOffset+=4;

				txtbuf.append("\n"+sfield+" at "+lastStartMark+", data start "+currentOffset+", data size: "+chunkSize+ " bytes\n");

				if(sfield.equals("data"))//get data size to compute duration later.
				{
					///
					af.dataOffset=currentOffset;
					af.dataSize=chunkSize;
				}

				if(sfield.equals("fact"))
				{
					if(chunkSize<4)
					{
						txtbuf.append("**** Not a valid fact chunk ****\n");
						p("file has invalid fact chunk");
					}

					chunkSize=0;
					for(int i=0;i<4;i++)
					{
						chunkSize += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					currentOffset+=4;
					txtbuf.append(indent+"(4) dwSampleLength: "+chunkSize+"\n");
				}
				else if(sfield.equals("fmt "))
				{// extract info from "format" chunk.
					if(chunkSize<16) //16,18 or 40
					{
						txtbuf.append("**** Not a valid fmt chunk ****\n");
						p("file has invalid fmt chunk");
						//
					}
					af.wFormatTag=dis.readUnsignedByte()+ dis.readUnsignedByte()*256;
					currentOffset+=2;

					if(af.wFormatTag==af.WAVE_FORMAT_LINEAR_PCM 
					|| af.wFormatTag==af.WAVE_FORMAT_EXTENSIBLE 
					|| af.wFormatTag==af.WAVE_FORMAT_IEEE_FLOAT
					|| af.wFormatTag==af.WAVE_FORMAT_ULAW
					|| af.wFormatTag==af.WAVE_FORMAT_ALAW
					)
					{
						af.isPCM=true;
					}
					if(af.wFormatTag==af.WAVE_FORMAT_LINEAR_PCM)
					{
						txtbuf.append(indent+"(2) af.wFormatTag: WAVE_FORMAT_LINEAR_PCM\n");
					}
					else if(af.wFormatTag==af.WAVE_FORMAT_EXTENSIBLE)
					{
						txtbuf.append(indent+"(2) af.wFormatTag: WAVE_FORMAT_EXTENSIBLE\n");
					}
					else if(af.wFormatTag==af.WAVE_FORMAT_IEEE_FLOAT)
					{
						txtbuf.append(indent+"(2) af.wFormatTag: WAVE_FORMAT_IEEE_FLOAT\n");
					}
					else if(af.wFormatTag==af.WAVE_FORMAT_ALAW)
					{
						txtbuf.append(indent+"(2) af.wFormatTag: WAVE_FORMAT_ALAW\n");
					}
					else if(af.wFormatTag==af.WAVE_FORMAT_ULAW)
					{
						txtbuf.append(indent+"(2) af.wFormatTag: WAVE_FORMAT_ULAW\n");
					}
					else
					{
						txtbuf.append(indent+"(2) af.wFormatTag: unknown format "+af.wFormatTag+"\n");
						///
					}
					af.nChannels=dis.readUnsignedByte();
					currentOffset++;
					dis.skipBytes(1);
					currentOffset++;
					txtbuf.append(indent+"(2) af.nChannels: "+af.nChannels+"\n");
					af.nSamplesPerSec=0;
					for(int i=0;i<4;i++)
					{ 
						af.nSamplesPerSec += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					currentOffset+=4;
					txtbuf.append(indent+"(4) af.nSamplesPerSec: "+af.nSamplesPerSec+"\n");
					af.nAvgBytesPerSec=0;
					for(int i=0;i<4;i++)
					{
						af.nAvgBytesPerSec += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					currentOffset+=4;
					txtbuf.append(indent+"(4) af.nAvgBytesPerSec: "+af.nAvgBytesPerSec+"\n");
					af.nBlockAlign=0;
					for(int i=0;i<2;i++)
					{
						af.nBlockAlign += dis.readUnsignedByte()*(int)Math.pow(256,i);
					}
					currentOffset+=2;
					txtbuf.append(indent+"(2) af.nBlockAlign: "+af.nBlockAlign+"\n");
					if(af.isPCM)
					{// if PCM or EXTENSIBLE format
						af.nBitsPerSample=dis.readUnsignedByte();
						currentOffset++;
						dis.skipBytes(1);
						currentOffset++;
						txtbuf.append(indent+"(2) af.nBitsPerSample: "+af.nBitsPerSample+"\n");
					}
					else
					{
						txtbuf.append("skipping 2 bytes at "+currentOffset+"\n");
						dis.skipBytes(2);
						currentOffset+=2;
					}
					/// 16
					txtbuf.append("skipping "+(chunkSize-16)+" bytes at "+currentOffset+"\n");

					dis.skipBytes(chunkSize-16);//skip over any extra bytes in format specific field.
					currentOffset+=(chunkSize-16);
				}//end sfield.equals("fmt ")
				else if(sfield.equals("LIST"))
				{
					String listtype="";
					for(int i=0;i<4;i++)
					{ 
						listtype+=(char)dis.readByte();
					}
					currentOffset+=4;

					//if(! ///skip over all the way, prevent any oddities
					if(listtype.equals("INFO"))
					{ //skip over LIST chunks which don't contain INFO subchunks
						dis.skipBytes(chunkSize-4);
						currentOffset+=(chunkSize-4);
						continue;
					}

					int listbytecount=4;
					txtbuf.append("\n------- INFO chunks -------\n");

					while(listbytecount < chunkSize)
					{//iterate over all entries in LIST chunk
						infofield="";
						infodescription="";
						infodata="";

						firstbyte=dis.readByte();
						currentOffset++;
						if(firstbyte==0)
						{//if previous data had odd bytecount, was padded by null so skip
							listbytecount++;
							continue;
						}
						infofield+=(char)firstbyte;//if we have a new chunk
						for(int i=0;i<3;i++)//get the remaining part of info chunk name ID
						{
							infofield+=(char)dis.readByte();
						}
						currentOffset+=3;
						int infochunksize=0;
						for(int i=0;i<4;i++)//get the info chunk data byte size
						{
							infochunksize += dis.readUnsignedByte()*(int)Math.pow(256,i);
						}
						listbytecount += (8+infochunksize);
						currentOffset+=4;

						for(int i=0;i<infochunksize;i++)
						{//get the info chunk data
							int byteRead=dis.readByte();
							currentOffset++;
							if(byteRead==0)//if null byte in string, ignore it
							{
								continue;
							}
							infodata+=(char)byteRead;
						}

						//don't populate for now
						infodescription=(String)RiffAudioFormat.listinfo.get(infofield);
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
					txtbuf.append("skpping at "+currentOffset+" "+chunkSize+" bytes\n");
					dis.skipBytes(chunkSize);
					currentOffset+=chunkSize;
				}
			}// end while (bytecount < riffData)
			//----------- End of chunk iteration -------------

			if(af.isPCM && af.dataSize>0)
			{//compute duration of PCM wave file
				long waveduration=1000L*af.dataSize/af.nAvgBytesPerSec;//in msec units
				long mins=waveduration/ 60000;	// integer minutes
				double secs=0.001*(waveduration % 60000);	//double secs.
				txtbuf.append("\nwav duration: "+mins+" mins "+secs+ " sec\n");
			}

			txtbuf.append("\nFinal RIFF data bytecount: "+bytecount+"\n");

			if((8+bytecount)!= (int)af.fileSize)
			{
				txtbuf.append("!!!!!!! Possible problem with file structure !!!!!!!!!\n");
			}
			else 
			{
				txtbuf.append("File chunk structure consistent with valid RIFF\n");
				//assume file/format to be valid
				af.isValid=true;
			}

			if(printDebug)
			{			
				p(txtbuf.toString());
			}

			//try find out subtype if float or int
			if(af.wFormatTag==af.WAVE_FORMAT_EXTENSIBLE)
			{
				byte[] bytes=null;
				try 
				{
					//read all that's not sample data at the beginning of a file and put to buffer
					bytes=new byte[(int)af.getDataOffset()];

					FileInputStream fis2=new FileInputStream(af.selectedFile);
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
					p("looking for byte sequence: "+bytesToHex( RiffAudioFormat.tail ));

					int found=bytesIndexOf(bytes, RiffAudioFormat.tail, 0);
					if(found!=-1)
					{
						p("found: "+bytes[found-2]+" "+bytes[found-1]);

						// -2  -1  pos from found
						//seq 1  0 -> dec 1
						//seq 3  0 -> dec 3  //float
						//seq 6  0 -> dec 6  //u-law
						//seq 7  0 -> dec 7  //u-law

						//others not of interest

						if(bytes[found-2]==1 && bytes[found-1]==0)
						{
							af.wSubFormat=af.WAVE_FORMAT_LINEAR_PCM;
						}
						else if(bytes[found-2]==3 && bytes[found-1]==0)
						{
							af.wSubFormat=af.WAVE_FORMAT_IEEE_FLOAT;
						}
						else if(bytes[found-2]==6 && bytes[found-1]==0)
						{
							af.wSubFormat=af.WAVE_FORMAT_ALAW;
						}
						else if(bytes[found-2]==7 && bytes[found-1]==0)
						{
							af.wSubFormat=af.WAVE_FORMAT_ULAW;
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

		return af;

	}//end parse
}//end class RiffAudioFileParser
