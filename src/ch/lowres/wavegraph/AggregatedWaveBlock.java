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
		paint(g2,waveHeight,baseLineY,0,false,false);
	}

//=======================================================
	public void paint(Graphics2D g2, float waveHeight, float baseLineY, float offsetX, 
		boolean displayRectified, boolean displayFilled)
	{
/*

                  .coord 0,0        ._max (+0.4) * waveHeight -> baseLineY - (+)max*waveHeight
                                    |
baseLineY---------ampl. 0-----------|-
                                    |
                                    |_min (-0.4) * waveHeight -> baseLineY - (-)min*waveHeight

rectified:

----------------------------

                      |
baseLineY-----------|-|----- displayed amplitude is max of abs(min), abs(max),   ( abs(max-min) )
              |     | |
              ||.|..|||
---------------------------- drawing starts a bottom of wavelane


*/
		if(!displayRectified)
		{
			float above=baseLineY-max*waveHeight;
			float below=baseLineY-min*waveHeight;

			if(below-above<1)
			{
				//line height 1 pixel minimum
				below=above-1;
			}

			if(!displayFilled)
			{
				g2.draw(new Line2D.Float(block+offsetX, below, block+offsetX, above));
			}
			else
			{
				float minOrZero=min;
				float maxOrZero=max;
				if(min>0 && max>0)
				{
					minOrZero=0;
				}
				if(min<0 && max<0)
				{
					maxOrZero=0;
				}
				above=baseLineY-maxOrZero*waveHeight;
				below=baseLineY-minOrZero*waveHeight;

				g2.draw(new Line2D.Float(block+offsetX, below, block+offsetX, above));
			}
		}
		else
		{
			float absmax=Math.abs(max);
			float absmin=Math.abs(min);
			float amplitude=Math.max(absmax,absmin)*waveHeight*2;

			float below=baseLineY+waveHeight;
			float above=below-amplitude;

			if(displayFilled)
			{
				g2.draw(new Line2D.Float(block+offsetX, below, block+offsetX, above));
			}
			else
			{
				g2.draw(new Line2D.Float(block+offsetX, above, block+offsetX, above-1));
			}
		}
	}//end paint
}//end class AggregatedWaveBlock
