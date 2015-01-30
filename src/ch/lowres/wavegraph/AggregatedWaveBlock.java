//tb/150119

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.geom.*;

//=======================================================
public class AggregatedWaveBlock
{
	//block number
	public long block=0;
	//samples covered
	public long samples=0;
	public int channel=0;
	//values
	public float min=0;
	public float max=0;
	//middle value of min, max
	public float avg=0;

//=======================================================
	public AggregatedWaveBlock(long block, long samples, int channel, float min, float max)
	{
		this.block=block;
		this.samples=samples;
		this.channel=channel;
		this.min=min;
		this.max=max;
		if(max-min>0)
		{
			this.avg=min+((max-min)/2f);
		}
		else //blocksize 1: same value
		{
			this.avg=max;
		}
	}

//=======================================================
	public int compare(AggregatedWaveBlock awb)
	{
		if(this.max>=awb.max)
		{
			return 1;
		}
		else
		{
			return -1;
		}
	}

//=======================================================
	public void paint(Graphics2D g2, float waveHeight, float baseLineY)
	{
/*

                  .coord 0,0        ._max (+0.4) * waveHeight -> baseLineY - (+)max*waveHeight
                                    |
baseLineY---------ampl. 0-----------|-
                                    |
                                    |_min (-0.4) * waveHeight -> baseLineY - (-)min*waveHeight

*/
		float above=baseLineY-max*waveHeight;
		float below=baseLineY-min*waveHeight;

		if(below-above<1)
		{
			//line height 1 pixel minimum
			below=above-1;
		}

		g2.draw(new Line2D.Float(block, below, block, above));
	}
}//end class AggregatedWaveBlock
