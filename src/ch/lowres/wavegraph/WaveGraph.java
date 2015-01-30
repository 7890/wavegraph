//tb/150119

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import java.io.*;
import java.util.*;

import javax.imageio.*;

import java.text.*;

//=======================================================
public class WaveGraph extends JPanel
{
	private static Main m;

	private ArrayList<AggregatedWaveBlock> blocks = new ArrayList<AggregatedWaveBlock>();
	private ArrayList<AggregatedWaveBlock> copy=new ArrayList<AggregatedWaveBlock>();
	private ArrayList<AggregatedWaveBlock> use=new ArrayList<AggregatedWaveBlock>();

	private float waveHeight;
	private float baseLineY;

	private boolean scanDone=false;
	private boolean clearDue=true;
	private boolean drawFull=false;
	private boolean suppressRepaint=false;

//=======================================================
	public WaveGraph()
	{
		this.setOpaque(true);
		this.setBackground(new Color(243,243,243));
	}

//=======================================================
	public void addBlock(AggregatedWaveBlock awb)
	{
		blocks.add(awb);
	}

//=======================================================
	public void clear()
	{
		//m.p("graph cleared");
		scanDone=false;
		clearDue=true;
		blocks.clear();
		copy.clear();
		use.clear();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
  		});
	}

//=======================================================
	public void suppressRepaint(boolean b)
	{
		suppressRepaint=b;
	}

//=======================================================
	public Dimension getPreferredSize()
	{
		//Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(m.mainframe.getGraphicsConfiguration());
		//m.windowHeight=(int)((m.mainframe.getHeight()-insets.top-insets.bottom));
		return new Dimension(
			(int)(m.width),
			(int)getHeight()
		);
	}

//=======================================================
	public void paintComponent(Graphics g)
	{
/*
1 vertical segement = 1 aggregated waveblock, one pixel wide 
-> min and max values are represented

connect vertical middle of segments to avoid non-continuous wave line drawing

vertical middle point of segment:
middle=max-((max-min)/2)

    _max
    |
    |___middle
    |
    |_ min
    |
    |?_______0

lookahead 1 segement to connect middle to middle

vgap=Math.min(c1,c2)
line=x, middle, x+0.5, max+vgap/2
needs special treatment for extrema (peaks, start, end)

       |
       |s2
       |_____
          ^
          | vgap
  ________.___
  |
  |s1

*/
		super.paintComponent(g);

		//remove all in viewport
		if(clearDue)
		{
			g.clearRect(0,0,getWidth(),getHeight());
			clearDue=false;
		}

		if(m.props==null || !m.props.isValid())
		{
			return;
		}

		if(suppressRepaint)
		{
			final Graphics2D g2 = (Graphics2D) g;

			g2.setColor(Color.gray);
			g2.setStroke(new BasicStroke(35));
			int h2=m.scrollpane.getHeight()/2;
			g2.draw(new Line2D.Float(0,h2,getWidth(),h2));
			return;
		}

		//**************************
		//create array of waveblocks to display for viewport

		if(!scanDone)
		{
			//prevent java.util.ConcurrentModificationException
			//only needed if not finished scanning!
			copy = new ArrayList<AggregatedWaveBlock>(blocks);
		}
		else
		{
			copy=blocks;
		}

		if(!drawFull)
		{
			//get sublist for current viewport
			use = new ArrayList<AggregatedWaveBlock>
			(
				copy.subList
				(
					Math.min
					(
						m.scrollOffset*m.props.getChannels(),
						copy.size()
					),
					Math.min
					(
						(int)(m.scrollOffset*m.props.getChannels()+m.visibleRect.getWidth() * m.props.getChannels()) ,
						copy.size()
					)
				)
			);
		}
		else //to export as image (don't limit to viewport)
		{
			use=copy;
		}

		//****
		//draw wave

		final Graphics2D g2 = (Graphics2D) g;

		//prevent the graph to "slip under" the scrollbar
		int sbHeight=0;
		if(m.scrollpane.getHorizontalScrollBar().isVisible())
		{
			sbHeight=m.scrollpane.getHorizontalScrollBar().getHeight();
		}

		//channels should never be 0 (div zero!)
		float waveHeightMax=(float) (( (m.scrollpane.getHeight()-sbHeight*2) /m.props.getChannels()) / 2);
		waveHeight=waveHeightMax*0.9f;

		float gap=waveHeightMax-waveHeight;

		g2.setStroke(new BasicStroke(1));

		AggregatedWaveBlock awb=null;
		AggregatedWaveBlock next=null;

		//*************************
		//main draw loop for blocks
		for(int i=0;i<use.size();i++)
		{
			awb=use.get(i);

			baseLineY=( (2*awb.channel-1) * waveHeightMax );

			//if high resolution == low sample per pixel value
			//connect avg points of blocks
			if(awb.samples<512)
			{
				//look ahead as long as not last
				if(i<=use.size()-1-m.props.getChannels())
				{
					next=use.get(i+m.props.getChannels());

					final long bl=awb.block;
					final float top=baseLineY-awb.avg*waveHeight;
					final float bottom=baseLineY-next.avg*waveHeight;

					g2.setColor(Color.black.brighter());//.brighter().brighter());
					g2.draw(new Line2D.Float(bl, top, bl+1, bottom));
				}
			}

			//paint vertical amplitude blocks
			g2.setColor(Color.black);
			awb.paint(g2, waveHeight, baseLineY);

			//paint +1/-1 limits
			g2.setColor(Color.red);
			g2.draw(new Line2D.Double(awb.block, baseLineY-waveHeight, 10, baseLineY-waveHeight));
			g2.draw(new Line2D.Double(awb.block, baseLineY-waveHeight, 10, baseLineY-waveHeight));
			g2.draw(new Line2D.Double(awb.block, baseLineY+waveHeight, 10, baseLineY+waveHeight));

			//paint zero-line
			g2.setColor(Color.gray);
			g2.draw(new Line2D.Float(awb.block, baseLineY, 10, baseLineY));

		} //end for every block avg


	}//end paintComponent

//=======================================================
	public void saveImage()
	{
		//dummy
	}
}//end class WaveGraph
