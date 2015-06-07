/*
 * Copyright 1999-2007 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/** 
 * AIFF file reader and writer
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */

//tb/150324 adaption

/*
http://www-mmsp.ece.mcgill.ca/documents/AudioFormats/AIFF/AIFF.html

NONE 	"not compressed" 		PCM big-endian				Apple Computer
sowt 	"not compressed" 		PCM little-endian			Apple Computer
fl32 	"32-bit floating point" 	IEEE 32-bit float 			Apple Computer
fl64 	"64-bit floating point" 	IEEE 64-bit float 			Apple Computer
alaw 	"ALaw 2:1" 			8-bit ITU-T G.711 A-law			Apple Computer
ulaw 	"µLaw 2:1"		 	8-bit ITU-T G.711 µ-law 		Apple Computer
ULAW 	"CCITT G.711 u-law" 		8-bit ITU-T G.711 µ-law (64 kb/s) 	SGI
ALAW 	"CCITT G.711 A-law" 		8-bit ITU-T G.711 A-law (64 kb/s) 	SGI
FL32 	"Float 32" 			IEEE 32-bit float 			SoundHack & Csound

https://developer.apple.com/library/ios/documentation/MusicAudio/Conceptual/CoreAudioOverview/SupportedAudioFormatsMacOSX/SupportedAudioFormatsMacOSX.html

extensions
AIFC (.aif, .aiff, .aifc):
data format:
BEI8, BEI16, BEI24, BEI32, BEF32, BEF64, 'ulaw', 'alaw', 'MAC3', 'MAC6', 'ima4' , 'QDMC', 'QDM2', 'Qclp', 'agsm'

AIFF (.aiff)
BEI8, BEI16, BEI24, BEI32

BEI:big-endian Integer
BEF:big-endian Float

//http://en.wikipedia.org/wiki/Audio_Interchange_File_Format

With the development of the Mac OS X operating system, Apple created a new type 
of AIFF which is, in effect, an alternative little-endian byte order format.

Because the AIFF architecture has no provision for alternative byte order, Apple 
used the existing AIFF-C compression architecture, and created a "pseudo-compressed" 
codec called sowt (twos spelled backwards). The only difference between a standard 
AIFF file and an AIFF-C/sowt file is the byte order; there is no compression involved 
at all.

Apple uses this new little-endian AIFF type as its standard on Mac OS X. When a file 
is imported to or exported from iTunes in "AIFF" format, it is actually AIFF-C/sowt 
that is being used. When audio from an audio CD is imported by dragging to the Mac 
OS X Desktop, the resulting file is also an AIFF-C/sowt. In all cases, Apple refers 
to the files simply as "AIFF", and uses the ".aiff" extension.

For the vast majority of users this technical situation is completely unnoticeable 
and irrelevant. The sound quality of standard AIFF and AIFF-C/sowt are identical, 
and the data can be converted back and forth without loss. Users of older audio 
applications, however, may find that an AIFF-C/sowt file will not play, or will 
prompt the user to convert the format on opening, or will play as static.

All traditional AIFF and AIFF-C files continue to work normally on Mac OS X (including 
on the new Intel-based hardware), and many third-party audio applications as well as 
hardware continue to use the standard AIFF big-endian byte order.

http://www-mmsp.ece.mcgill.ca/documents/AudioFormats/AIFF/Docs/AIFF-1.3.pdf
*/

/*
$ echo -n "NONE" | hexdump -C
00000000 4e 4f 4e 45		|NONE|
00000004
*/

package ch.lowres.wavegraph;

import java.io.*;

//=======================================================
public class AiffAudioFileParser extends AudioFileParser
{
	static final int FILE_START_MAGIC	=0x464f524d; // 'FORM'
	static final int AIFC_MAGIC		=0x41494643; // 'AIFC'
	static final int AIFF_MAGIC		=0x41494646; // 'AIFF'
	static final int FVER_MAGIC		=0x46564552; // 'FVER'sion
	static final int FVER_TIMESTAMP		=0xA2805140; // timestamp of last AIFF-C update
	static final int COMM_MAGIC		=0x434f4d4d; // 'COMM'on
	static final int SSND_MAGIC		=0x53534e44; // 'SSND'
	static final int MARK_MAGIC		=0x4d41524b; // 'MARK'
	static final int INST_MAGIC		=0x494e5354; // 'INST'rument
	static final int AUTH_MAGIC		=0x41555448; // 'AUTH'or
	static final int COPYRIGHT_MAGIC	=0x28632920; // '(c) '
	static final int NAME_MAGIC		=0x4e414d45; // 'NAME'
	static final int ANNO_MAGIC		=0x414e4e4f; // 'ANNO'
	static final int MIDI_MAGIC		=0x4d494449; // 'MIDI'
	static final int AESD_MAGIC		=0x41455344; // 'AESD' audio recording
	static final int APPL_MAGIC		=0x4150504c; // 'APPL'ication

	//compression codes
	static final int AIFC_NONE 		=0x4e4f4e45; // 'NONE' PCM big-endian
	static final int AIFC_SOWT 		=0x736f7774; // 'sowt' PCM little-endian
	 	static final int AIFC_ACE2		=0x41434532; // 'ACE2' ACE 2:1 compression
 		static final int AIFC_ACE8		=0x41434538; // 'ACE8' ACE 8:3 compression
	 	static final int AIFC_MAC3		=0x4d414333; // 'MAC3' MACE 3:1 compression
	 	static final int AIFC_MAC6		=0x4d414336; // 'MAC6' MACE 6:1 compression
	static final int AIFC_ALAW		=0x616c6177; // 'alaw' ITU G.711 a-Law
	static final int AIFC_ULAW		=0x756c6177; // 'ulaw' ITU G.711 u-Law
	 	static final int AIFC_IMA4		=0x696d6134; // 'ima4' IMA ADPCM
	 	static final int AIFC_QDMC		=0x51444d43; // 'QDMC' QDesign
	 	static final int AIFC_QDM2		=0x51444d32; // 'QDM2' Qualcomm PureVoice
	 	static final int AIFC_Qclp		=0x51636c70; // 'Qclp' Qualcomm QCELP
	 	static final int AIFC_agsm		=0x6167736d; // 'agsm' Apple GSM 10:1
	static final int AIFC_FL32 		=0x666c3332; // 'fl32'
	static final int AIFC_FL32_2		=0x464c3332; // 'FL32'
	static final int AIFC_FL64 		=0x666c3634; // 'fl64'

	static final int AIFC_ALAW_2		=0x414c4157; // 'ALAW' //difference to alaw?
	static final int AIFC_ULAW_2		=0x554c4157; // 'ULAW' //difference to ulaw?

//	static final int AIFF_HEADERSIZE	=54;

//=======================================================
	public AudioFormat parse(String file)
	{
		af=new AiffAudioFormat();
		af.selectedFile=file;

		try
		{
			fis=new FileInputStream(af.selectedFile);
			getCOMM(fis);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(fis!=null)
			{
				try
				{
					fis.close();
				}
				catch(Exception e){}
			}
		}

		return af;

	}//end parse

//=======================================================
	private void getCOMM(InputStream is) throws Exception
	{
		dis=new DataInputStream(is);

		if(dis.available()<=af.minimalHeaderSize)
		{
			throw new Exception("file size too small for an AIFF file");
		}

		///wrong
		af.fileSize=dis.available();

		// assumes a stream at the beginning of the file which has already
		// passed the magic number test...
		// leaves the input stream at the beginning of the audio data
		int fileRead=0;
		int dataLength=0;

		// Read the magic number
		int magic=dis.readInt();

		if (magic !=FILE_START_MAGIC)
		{
			throw new Exception("not an AIFF file");
		}

		int length=dis.readInt();
		int iffType=dis.readInt();
		fileRead+=12;

		int totallength=0;
		if(length <=0 )
		{
			throw new Exception("total length can't be <=0");
		}
		else
		{
			totallength=length + 8;
		}

		p("totallength "+totallength);

		// Is this an AIFC or just plain AIFF file.
		boolean aifc=false;

		if (iffType==AIFC_MAGIC)
		{
			aifc=true;
		}
		else if(iffType !=AIFF_MAGIC)
		{
			throw new Exception("not an AIFF file");
		}

		int startOfSampleDataOffset=0;
		int totalSampleDataBytes=0;
		int dataOffset=0;
		int blocksize=0;
		int frameSize=0;

		// Loop through the AIFF chunks until
		// we get to the SSND chunk.
		boolean ssndFound=false;
		boolean endOfFile=false;
		boolean commFound=false;
		//at least look for SSND and COMM chunks until found or EOF
		while ( (!ssndFound || !commFound) && !endOfFile)
		{
			if(dis.available()<8)
			{
				//p("less than 8 available!");
				endOfFile=true;
				continue;
			}

			// Read the chunk name
			int chunkName=dis.readInt();
			int chunkLen=dis.readInt();
			fileRead+=8;

			int chunkRead=0;

			// Switch on the chunk name.
			switch (chunkName)
			{
				case FVER_MAGIC:
					// Ignore format version for now.
					p("==CHUNK...FVER @ "+fileRead+", chunklen "+chunkLen);
					break;
				//==============================================
				case COMM_MAGIC:
					commFound=true;
					p("==CHUNK...COMM @ "+fileRead+", chunklen "+chunkLen);

					// AIFF vs. AIFC
					if ((!aifc && chunkLen < 18) || (aifc && chunkLen < 22))
					{
						throw new Exception("Invalid AIFF/COMM chunksize: "+chunkLen); //UnsupportedAudioFileException("Invalid AIFF/COMM chunksize");
					}
					// Read header info.
					int channels=dis.readShort();
					af.nChannels=channels;
					p(" channels "+channels);

					dis.readInt();
					int sampleSizeInBits=dis.readShort();
					af.nBitsPerSample=sampleSizeInBits;
					p(" samplesizebits "+sampleSizeInBits);

					float sampleRate=(float) read_ieee_extended(dis);
					af.nSamplesPerSec=(int)sampleRate;
					p(" samplerate "+sampleRate);

					chunkRead+=(2 + 4 + 2 + 10);

					// If this is not AIFC then we assume it's
					// a linearly encoded file.
					//AudioFormat.Encoding encoding=AudioFormat.Encoding.PCM_SIGNED;

					if (aifc)
					{
						af.fileType=af.AIFC_FILE;

						int enc=dis.readInt(); chunkRead+=4;
						switch (enc)
						{
							case AIFC_NONE:
								af.wFormatTag=af.WAVE_FORMAT_LINEAR_PCM;
								af.isPCM=true;
								p(" encoding AIFC_NONE");
								break;
							case AIFC_SOWT:
								//LITTlE ENDIAN
								///
								af.wFormatTag=af.WAVE_FORMAT_LINEAR_PCM;
								af.isPCM=true;
								p(" encoding AIFC_SOWT");
								break;
							case AIFC_ALAW:
								af.wFormatTag=af.WAVE_FORMAT_ALAW;
								af.isPCM=true;
								p(" encoding AIFC_ALAW");
								break;
							case AIFC_ULAW:
								af.wFormatTag=af.WAVE_FORMAT_ULAW;
								af.isPCM=true;
								p(" encoding AIFC_ULAW");
								break;
							case AIFC_ALAW_2:
								///
								af.wFormatTag=af.WAVE_FORMAT_ALAW;
								p(" encoding AIFC_ALAW_2");
								af.isPCM=true;
								break;
							case AIFC_ULAW_2:
								///
								af.wFormatTag=af.WAVE_FORMAT_ULAW;
								p(" encoding AIFC_ULAW_2");
								af.isPCM=true;
								break;
							case AIFC_FL32:
								af.wFormatTag=af.WAVE_FORMAT_IEEE_FLOAT;
								af.isPCM=true;
								p(" encoding AIFC_FL32");
								break;
							case AIFC_FL64:
								af.wFormatTag=af.WAVE_FORMAT_IEEE_FLOAT;
								af.isPCM=true;
								p(" encoding AIFC_FL64");
								break;
							default:
								af.isPCM=false;
								//throw new Exception("Invalid AIFF encoding");
						}//end switch enc
					}
					else
					{
						af.fileType=af.AIFF_FILE;
						af.wFormatTag=af.WAVE_FORMAT_LINEAR_PCM;
						af.isPCM=true;
						p(" encoding AIFF");
					}

					frameSize=calculatePCMFrameSize(sampleSizeInBits, channels);
					af.nBlockAlign=frameSize;
					p(" framesize "+frameSize);
					break; //end case COMM_MAGIC
				case MARK_MAGIC:
					p("==CHUNK...MARK @ "+fileRead+", chunklen "+chunkLen);
					break;
				case INST_MAGIC:
					p("==CHUNK...INST @ "+fileRead+", chunklen "+chunkLen);
					break;
				case AUTH_MAGIC:
					p("==CHUNK...AUTH @ "+fileRead+", chunklen "+chunkLen);
					break;
				case COPYRIGHT_MAGIC:
					p("==CHUNK...COPYRIGHT @ "+fileRead+", chunklen "+chunkLen);
					break;
				case NAME_MAGIC:
					p("==CHUNK...NAME @ "+fileRead+", chunklen "+chunkLen);
					break;
				case ANNO_MAGIC:
					p("==CHUNK...ANNO @ "+fileRead+", chunklen "+chunkLen);
					break;
				case MIDI_MAGIC:
					p("==CHUNK...MIDI @ "+fileRead+", chunklen "+chunkLen);
					break;
				case AESD_MAGIC:
					p("==CHUNK...AESD @ "+fileRead+", chunklen "+chunkLen);
					break;
				case APPL_MAGIC:
					p("==CHUNK...APPL @ "+fileRead+", chunklen "+chunkLen);
					break;
				//==============================================
				case SSND_MAGIC:
					p("==CHUNK...SSND @ "+fileRead+", chunklen "+chunkLen);
					// Data chunk.
					// we are getting *weird* numbers for chunkLen sometimes;
					// this really should be the size of the data chunk....
					dataOffset=dis.readInt();
					blocksize=dis.readInt();
					chunkRead+=8;

					p("dataoffset "+dataOffset);
					p("blocksize "+blocksize);

					///blockSize not considered
					startOfSampleDataOffset=fileRead+8	+dataOffset;
					totalSampleDataBytes=chunkLen-8		-dataOffset;

					///
					af.dataSize=totalSampleDataBytes;
					af.dataOffset=startOfSampleDataOffset;

					p("==SAMPLE DATA STARTING @ byte offset "+startOfSampleDataOffset+", total bytes "+totalSampleDataBytes);

					// okay, now we are done reading the header. we need to set the size
					// of the data segment. we know that sometimes the value we get for
					// the chunksize is absurd. this is the best i can think of:if the
					// value seems okay, use it. otherwise, we get our value of
					// length by assuming that everything left is the data segment;
					// its length should be our original length (for all AIFF data chunks)
					// minus what we've read so far.
					// $$kk: we should be able to get length for the data chunk right after
					// we find "SSND." however, some aiff files give *weird* numbers. what
					// is going on??

					if (chunkLen < length)
					{
						dataLength=chunkLen - chunkRead;
					}
					else
					{
						// $$kk: 11.03.98: this seems dangerous!
						dataLength=length - (fileRead + chunkRead);
					}
					ssndFound=true;
					break;
				default:
					p("==CHUNK...UNKNOWN @ "+fileRead+", chunklen "+chunkLen+( (chunkLen % 2==1) ? " (+1)" : "") );
					break;
			} // end switch
			/*
			text
			contains the comment itself. This text must be padded with a byte at the end to insure that it
			is an even number of bytes in length. This pad byte, if present, is not included in
			count
			*/

			//skip the remainder of this chunk
			fileRead+=chunkRead;
 
			//allow both orders of comm, ssnd; ssnd, comm
			if ((!ssndFound && commFound) || 
				(ssndFound && !commFound) || 
				(!ssndFound && !commFound) )
			{
				int toSkip=chunkLen - chunkRead;
				//if odd, pad to even number
				if(toSkip % 2==1)
				{
					toSkip++;
				}
				//p("toskip "+toSkip);

				if (toSkip > 0)
				{
					fileRead+=toSkip;
					dis.skipBytes(toSkip);
					for(int k=0;k<toSkip;k++)
					{
						//p(" skip "+(char)dis.read());
					}
				}
			}
		} // end while

		if(!ssndFound)
		{
			throw new Exception("missing SSND chunk! (no sample data found)");
		}
		if(!commFound)
		{
			throw new Exception("missing COMM chunk! (no meta data found)");
		}
		if(frameSize==0)
		{
			throw new Exception("frameSize can't be 0");
		}

		p("frames: "+totalSampleDataBytes / frameSize);

		///
		af.isValid=true;
	}//end getCOMM

//=======================================================
	 /** Calculates the frame size for PCM frames.
	* Note that this method is appropriate for non-packed samples.
	* For instance, 12 bit, 2 channels will return 4 bytes, not 3.
	* @param sampleSizeInBits the size of a single sample in bits
	* @param channels the number of channels
	* @return the size of a PCM frame in bytes.
	*/
	protected static int calculatePCMFrameSize(int sampleSizeInBits, int channels) 
	{
		return ((sampleSizeInBits + 7) / 8) * channels;
	}
}//end class AiffFileReader
