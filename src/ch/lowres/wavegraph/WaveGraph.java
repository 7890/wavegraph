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
public class WaveGraph extends JPanel implements MouseMotionListener, MouseListener
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

	//primitive, single range
	//0: press 1: current drag end, release 2: current mouse
	private Point[] positions=new Point[3];
	private boolean mousePressed=false;
	private boolean dragOngoing=false;

	private final static Stroke stroke1=new BasicStroke(1);
	private final static Stroke stroke2=new BasicStroke(2);
	private final static Stroke stroke35=new BasicStroke(35);

//=======================================================
	public WaveGraph()
	{
		this.setOpaque(true);
		setBackground(Colors.wave_canvas_background);

		positions[0]=new Point(0,0);
		positions[1]=new Point(0,0);
		positions[2]=new Point(0,0);

		addMouseListener(this);
		addMouseMotionListener(this);
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
*/

/////
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
			//m.p("repaint was suppressed");
			final Graphics2D g2 = (Graphics2D) g;

			g2.setColor(Color.gray);
			g2.setStroke(stroke35);
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


		final BasicStroke strokeChannelBackground = 
			new BasicStroke(2*waveHeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
		g2.setStroke(strokeChannelBackground);

		//draw channel background
		for(int w=0;w<m.props.getChannels();w++)
		{
			baseLineY=( (2*(w+1)-1) * waveHeightMax );

			g2.setColor(Colors.wave_background);
			g2.draw(new Line2D.Double(m.scrollOffset, baseLineY, m.scrollOffset+m.visibleRect.getWidth(), baseLineY));
		}

		g2.setStroke(stroke1);

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

					g2.setColor(Colors.wave_foreground.brighter());
					g2.draw(new Line2D.Float(bl, top, bl+1, bottom));
				}
			}

			//paint vertical amplitude blocks
			g2.setColor(Colors.wave_foreground);
			awb.paint(g2, waveHeight, baseLineY);
		} //end for every block avg

		//draw on top channel limits, zero lines
		for(int w=0;w<m.props.getChannels();w++)
		{
			baseLineY=( (2*(w+1)-1) * waveHeightMax );

			//1
			g2.setStroke(stroke1);
			g2.setColor(Colors.wave_delimiter_top);
			g2.draw(new Line2D.Double(m.scrollOffset, baseLineY-waveHeight, m.scrollOffset+m.visibleRect.getWidth(), baseLineY-waveHeight));

			//-1
			g2.setStroke(stroke2);
			g2.setColor(Colors.wave_delimiter_bottom);
			g2.draw(new Line2D.Double(m.scrollOffset, baseLineY+waveHeight, m.scrollOffset+m.visibleRect.getWidth(), baseLineY+waveHeight));

			//paint zero-line
			g2.setStroke(stroke1);
			g2.setColor(Colors.wave_zeroline);
			g2.draw(new Line2D.Double(m.scrollOffset, baseLineY, m.scrollOffset+m.visibleRect.getWidth(), baseLineY));
		}
	}//end paintComponent

///need global var to suppress mouse processing

//=======================================================
	public void mouseMoved(MouseEvent e)
	{
		positions[2]=e.getPoint();//mouse
		//tmp
		if(m.haveValidFile)
		{
			m.mousePositionInGraph.setText("|  Pos "+e.getPoint().x);
		}
	}

//=======================================================
	public void mouseDragged(MouseEvent e)
	{
		if(mousePressed) //button1
		{
			dragOngoing=true;
			positions[1]=e.getPoint();//current end
			positions[2]=e.getPoint();//current mouse
			//repaint();

			//drag-move on waveform
			int diff=positions[1].x-positions[0].x;
			m.scrollbar.setValue(m.scrollbar.getValue()-diff);
		}
	}

//=======================================================
	public void mousePressed(MouseEvent e)
	{
//		m.p("Mouse pressed; # of clicks: "+ e.getClickCount())+" "+e);
		if(e.getButton()==e.BUTTON1)
		{
			mousePressed=true;
			positions[0]=e.getPoint();//press
			positions[1]=e.getPoint();//current end (same)
			positions[2]=e.getPoint();//current end (same)

			//could be start of drag! but not sure yet
			//single pixel repaint needed!
			//repaint();
		}
	}

//=======================================================
	public void mouseReleased(MouseEvent e)
	{
//		m.p("released; # of clicks: "+ e.getClickCount()+" "+e);
		if(e.getButton()==e.BUTTON1)
		{
			mousePressed=false;
			dragOngoing=false;
			positions[1]=e.getPoint();//end / released
			//repaint();
		}
	}

//=======================================================
	public void mouseEntered(MouseEvent e)
	{
//		m.p("entered "+e);
	}

//=======================================================
	public void mouseExited(MouseEvent e)
	{
//		m.p("exited "+ e);
	}

//=======================================================
	public void mouseClicked(MouseEvent e)
	{
//		m.p("clicked (# of clicks: "+ e.getClickCount() + ")"+ e);
	}

//=======================================================
	public void saveImage()
	{
		//dummy
	}
}//end class WaveGraph
