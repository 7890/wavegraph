//tb/150117

package ch.lowres.wavegraph;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

//=======================================================
class WaveScanner extends Observable implements Runnable
{
	public final static int UNKNOWN=-1; 
	public final static int STARTED=0; 
	public final static int INITIALIZED=1;
	public final static int ABORTED=2;
	public final static long DATA_AVAILABLE=3;
	public final static int DONE=4;
	public final static int EXCEPTION=5;

	private int status=UNKNOWN;

	private String fileName="";

	private AudioFormat props=new AudioFormat();

	private FileInputStream fis;
	private FileChannel fc;
	private MappedByteBuffer mbb;

	private long outputWidth=0; //pixels
	private long blockSize=0; //frames per block (one block per channel)
	private long cycles=0; //cycles needed to aggregate blocks (for all channels)

	private boolean abortRequested=false;

	private Thread thread; //to start scanner in thread
	private WaveGraph graph; //add aggregated wave blocks to this graph

	//test, default
	private boolean isBigEndian=false;

//=======================================================
	public WaveScanner(WaveGraph graph)
	{
		this.graph=graph;
	}

//=======================================================
	private void readHeader()
	{
		AudioFileParser afp=AudioFormat.createParser(fileName);
		props=afp.parse(fileName);

		if(!props.isValid())
		{
			p("file "+fileName+" can not be parsed.");
			return;
		}

		props.print();
	}

//=======================================================
	public AudioFormat getProps(String file)
	{
		fileName=file;
		readHeader();
		return props;
	}

//=======================================================
	public AudioFormat getProps()
	{
		if(props==null)
		{
			///
			p("WaveScanner: props was null");
			props= new AudioFormat();
		}
		return props;
	}

//=======================================================
	public void abort()
	{
		abortRequested=true;
	}

//=======================================================
	public void reset()
	{
		if(status==STARTED || status==INITIALIZED)
		{
			abort();
			try{Thread.sleep(100);}catch(Exception e){}
		}
		fileName="";
		outputWidth=0;
		blockSize=0;
		cycles=0;
		props=new AudioFormat();
		abortRequested=false;
		status=UNKNOWN;
	}

//=======================================================
	public void scanData(long pixelsTotalWidth) throws Exception
	{
		scanData(0,props.getFrameCount(),pixelsTotalWidth);
	}

//=======================================================
/*
	fromPos: framePosition, relative to start of sample data (absolute pos 0 of data chunk)
	frames: number of frames starting at framePos!= byte position
		one frame: a collection of datapoints ("samples") including all channels for a given position
	pixelsTotalWidth: desired with of graph (consisting of a series of vertical 1 pixel-wide lines representing data)
*/

	public void scanData(long fromPos, long frames, long pixelsTotalWidth) throws Exception
	{
/*
from pos    0         1         2
------------|---------|---------|...
frame            1         2


ch1 sample       1         1
ch2 sample       2         2
..

*/
		if(!props.isValid())
		{
			p("file "+fileName+ " was not valid.");
			return;
		}

		long targetWidth=pixelsTotalWidth;

		long fpp=1;
		double fpp_=(double) frames/targetWidth;
		if(fpp_<1)
		{
			fpp=1;
			outputWidth=frames;
		}
		else
		{
			fpp=(long)Math.floor(fpp_);
			outputWidth=(long)Math.ceil((double)frames/fpp);
		}

		blockSize=fpp;

		p("target width "+targetWidth+" would be fpp "+fpp_+", but using "+fpp+" -> effective width "+outputWidth );

		try
		{
			fis = new FileInputStream(new File(fileName));
			fc=fis.getChannel();

			///need to limit range

			//read data after header
			mbb=fc.map(FileChannel.MapMode.READ_ONLY,
				props.getDataOffset()
					+fromPos*props.getBlockAlign(),
				frames*props.getBlockAlign());

			isBigEndian=false;
			if(props.getFileType()==props.AIFC_FILE
				|| props.getFileType()==props.AIFF_FILE
			)
			{
				isBigEndian=true;
			}

			if(isBigEndian)
			{
				p("***setting byteorder BIG_ENDIAN");
				mbb.order(ByteOrder.BIG_ENDIAN);
			}
			else
			{
				p("***setting byteorder LITTLE_ENDIAN");
				//assume RIFF data is little endian
				mbb.order(ByteOrder.LITTLE_ENDIAN);
			}

			//p("mapped byte buffer info: "+mbb.toString());

			abortRequested=false;
			thread=new Thread(this);

			//p("scanning sample data...");
			thread.start();
		}
		catch(Exception e)
		{
			status=EXCEPTION;
			setChanged();
			notifyObservers(EXCEPTION);
			clearChanged();
		}
		finally
		{
			fc.close();
			fis.close();
		}
	}//end scanData

//=======================================================
	public long getBlockSize()
	{
		return blockSize;
	}

//=======================================================
	public long getOutputWidth()
	{
		return outputWidth;
	}

//=======================================================
	public long getCycles()
	{
		return cycles;
	}

//=======================================================
	public void run()
	{
		//sanity check
		if(blockSize<=0 || outputWidth<=0 || !props.isValid() || !props.isPCM() || props.getChannels()<=0 || props.getFrameCount()<=0)
		{
			p("no work to do: width==0, props invalid or no samples to proess.");
			return;
		}

		status=STARTED;
		setChanged();
		notifyObservers(STARTED);
		clearChanged();

		float currentValue=0;

		float[] blockMin = new float[props.getChannels()];
		float[] blockMax = new float[props.getChannels()];

		long block=0;

		///
		cycles=(int)mbb.capacity()/props.getBlockAlign();

		status=INITIALIZED;
		setChanged();
		notifyObservers(INITIALIZED);
		clearChanged();

		for(int i=0;i<cycles;i++)
		{
			for(int channel=0;channel<props.getChannels();channel++)
			{
				currentValue=nextSampleValue(mbb);

				blockMin[channel]=Math.min(blockMin[channel],currentValue);
				blockMax[channel]=Math.max(blockMax[channel],currentValue);

				//don't run on first cycle, run every blockSize or last block
				if(i!=0 && ((i+1) % blockSize == 0
					|| 
					(i==cycles-1))
				)
				{
					if(i==cycles-1 && (i+1)%blockSize!=0 )
					{
						long remainder=(i+1)%blockSize;
						p("remainder ch "+(channel+1)+" "+remainder);

						//add as new aggregated block to graph
						graph.addBlock(
							new AggregatedWaveBlock
							(block,
							remainder,
							(channel+1),
							blockMin[channel],
							blockMax[channel])
						);
					}
					else
					{
						//add as new aggregated block to graph
						graph.addBlock(
							new AggregatedWaveBlock
							(block,
							blockSize,
							(channel+1),
							blockMin[channel],
							blockMax[channel])
						);
					}

					setChanged();
					//send block no with offset 1000 + DATA_AVAILABLE
					//notifyObservers(new Long(DATA_AVAILABLE+1000+block));
					notifyObservers(block);
					clearChanged();

					//"initialize"
					blockMin[channel]=1000;
					blockMax[channel]=-1000;

					//if last channel
					if(channel==props.getChannels()-1)
					{
						block++;
					}

					//allow abort after one multichannel block
					if(abortRequested)
					{
						status=ABORTED;
						setChanged();
						notifyObservers(ABORTED);
						clearChanged();
						return;
					}
				}
			}//end for channels
		}//end for cap

		status=DONE;
		setChanged();
		notifyObservers(DONE);
		clearChanged();
	}//end run

//=======================================================
	private final byte[] b2=new byte[2];
	private final byte[] b3=new byte[3];
	private final byte[] b4=new byte[4];
	private final byte[] b8=new byte[8];

	public float nextSampleValue(MappedByteBuffer buf)
	{
		//might be half-baked, output looks ~correct
		//ev. clamp integer-based to -1/+1

		//1 byte per sample and channel
		if(props.getBitsPerSample()==8)
		{
			if(props.getTag()==props.WAVE_FORMAT_ULAW)
			{
				return ToFloat.ulaw(buf.get());
			}
			else if(props.getTag()==props.WAVE_FORMAT_ALAW)
			{
				return ToFloat.alaw(buf.get());
			}
			else
			{
				if(props.getFileType()==props.AIFF_FILE
					|| props.getFileType()==props.AIFC_FILE
				)
				{
					return ToFloat.signedByte(buf.get());
				}
				else if(props.getFileType()==props.RIFF_FILE)
				{
					//RIFF sample values up to 8 bit are supposed to be unsigned (?)
					return ToFloat.unsignedByte(buf.get());
				}
				//else
				//{
				//}
			}
		}

///
//add 12 bit

		//2 bytes per sample and channel
		else if(props.getBitsPerSample()==16)
		{
			buf.get(b2);

			if(props.getTag()==props.WAVE_FORMAT_ULAW)
			{
//				return ToFloat.ulaw( b2 );
///dummy
return 0.2f;
			}
			else if(props.getTag()==props.WAVE_FORMAT_ALAW)
			{
//				return ToFloat.alaw( b2 );
///dummy
return 0.3f;
			}
			else
			{
				return ToFloat.signed16(b2,isBigEndian);
			}
		}
		//3 bytes per sample and channel
		else if(props.getBitsPerSample()==24)
		{
			buf.get(b3);
			return ToFloat.signed24(b3,isBigEndian);
		}
		//4 bytes per sample and channel
		else if(props.getBitsPerSample()==32)
		{
			//test whether integer or float
			if(props.getTag()==props.WAVE_FORMAT_LINEAR_PCM
				|| props.getSubFormat()==props.WAVE_FORMAT_LINEAR_PCM)
			{
				buf.get(b4);
				return ToFloat.signed32(b4,isBigEndian);
			}
			else //assume float
			//props.WAVE_FORMAT_IEEE_FLOAT, props.getSubFormat()==props.WAVE_FORMAT_IEEE_FLOAT
			{
				return buf.getFloat();
			}
		}
		//8 bytes per sample and channel
		else if(props.getBitsPerSample()==64)
		{
			//test whether integer or float
			if(props.getTag()==props.WAVE_FORMAT_LINEAR_PCM
				|| props.getSubFormat()==props.WAVE_FORMAT_LINEAR_PCM)
			{
				buf.get(b8);
				return ToFloat.signed64(b8,isBigEndian);
			}
			else //assume double, return float
			{
				return (float)buf.getDouble();
			}
		}
		//dummy
		return 0;
	}//end nextSampleValue

//=======================================================
	public static void p(String s)
	{
		System.out.println(s);
	}
}//end class WaveScanner
