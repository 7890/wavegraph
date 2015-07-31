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
traditional:


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


differential:

compare max values of two waveblocks, take absolute difference -> new max
compare min values of two waveblocks, take absolute negative difference -> new min

"inter-waveblock"


                 traditional                 rectified


                                   3    3
                 2                 |    |    |                   |    |
                 |    1       0    |1   |1   |         |         |    |
ch1              |____|___0___.___________   |____|____|____.____|____|__
                 |    0   |   0
                -1        |
                         -2


                          2
                 1        |   1              |         |    |
ch2              |____0___|___|____.______   |____|____|____|____.____|__
                 |    |   0   |
                -1   -1      -1         |-1
                                       -2
                                                                      |
                                                                      |
                                        4                             |
                                   3    |              |              |
                          2        |    |              |         |    |
                 1    1   |   1    |1   |         |    |    |    |    |
diff             |____|___|___|_________|__  |____|____|____|____|____|__
                 0    |   |   |         |                             ^
                     -1   |  -1         |                       high bar means:
                         -2             |                       large difference
                                       -3                       in min/max values of compared
                                                                waveblocks

1=|2-1|  0=-|-1 - -1|
1=|1-0| -1=-| 0 - -1|
2=|0-2| -2=-|-2 -  0|
1=|0-1| -1=-| 0 - -1|
2=|2-0|  1=-| 1 -  0|

x=| 3 - -1| y=-| 1 - -2|
x=4         y=-3

(not implemented)


"non-linear time"

sort by
	max: blocks with highest max values first
	min: blocks with most negative min values first
	avg: blocks with highest avg values first
	invert: reverse output

(not implemented)

*/
		if(!displayRectified)//traditional
		{
			float above=baseLineY-max*waveHeight;
			float below=baseLineY-min*waveHeight;

			if(below-above<1)
			{
				//line height 1 pixel minimum
				below=above-1;
			}

			if(!displayFilled)//lines
			{
				g2.draw(new Line2D.Float(block+offsetX, below, block+offsetX, above));
			}
			else//filled
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
		else //rectified view
		{
			float absmax=Math.abs(max);
			float absmin=Math.abs(min);
			float amplitude=Math.max(absmax,absmin)*waveHeight*2;

			if(!displayFilled)//lines
			{
				float above=baseLineY+waveHeight-amplitude;
				float below=above-Math.abs(max-min);

				g2.draw(new Line2D.Float(block+offsetX, below, block+offsetX, above));
			}
			else//filled
			{

				float below=baseLineY+waveHeight;
				float above=below-amplitude;

				g2.draw(new Line2D.Float(block+offsetX, below, block+offsetX, above));
			}
		}
	}//end paint
}//end class AggregatedWaveBlock
