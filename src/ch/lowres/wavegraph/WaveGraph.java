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
	//0: press 1: current range end (mouse), release
	private Point[] positions=new Point[2];
	private Point[] positionsCanvasDrag=new Point[2];
	private Point[] positionsSelectionRange=new Point[2];

	private int temporaryMarker=0;

	//offset of mouse press position to positionsSelection[0]
	private int offsetToSelectionStart=0;

	private final int DRAG_MOVE_CANVAS=0;
	private final int DRAG_CREATE_SELECTION=1;
	private final int DRAG_MOVE_SELECTION=2;

	private int dragType=DRAG_MOVE_CANVAS;

	private boolean dragOngoing=false;
	private boolean mousePressed=false;
	private boolean mouseInside=false;

	private final static Stroke stroke1=new BasicStroke(1);
	private final static Stroke stroke2=new BasicStroke(2);
	private final static Stroke stroke35=new BasicStroke(35);

	private boolean singlePixelChange=false;
	//tmp, remember last drawn mouse position
	private long positionsPrev_x=0;

	//handle repaint while scan is ongoing
	private long firstCounterWhileLoading=0;
	private boolean waitWithRepaintWhileLoading=false;

	private Cursor cursor = Cursor.getDefaultCursor();

	//used to draw "into" and re-use if viewport doesn't change
	private BufferedImage img;

//=======================================================
	public WaveGraph()
	{
		setOpaque(false);

		JViewport viewport = new GraphViewport();
		viewport.setView(this);
		m.scrollpane.setViewport(viewport);
		resetPositions();

		addMouseListener(this);
		addMouseMotionListener(this);
	}

//=======================================================
	public void addBlock(AggregatedWaveBlock awb)
	{
		blocks.add(awb);
	}

//=======================================================
	public void resetPositions()
	{
		positions[0]=new Point(0,0);
		positions[1]=new Point(0,0);

		positionsCanvasDrag[0]=new Point(0,0);
		positionsCanvasDrag[1]=new Point(0,0);

		positionsSelectionRange[0]=new Point(0,0);
		positionsSelectionRange[1]=new Point(0,0);
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
		resetPositions();
		firstCounterWhileLoading=0;
		waitWithRepaintWhileLoading=false;

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
		});
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

////all ops need limitation to >=0 <=width values

//=======================================================
	public void doubleSelectionRangeRight()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[1].x+=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
		}
		else
		{
			positionsSelectionRange[0].x+=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
		}
		singlePixelChange=true;
		repaint();
	}

//=======================================================
	public void halveSelectionRangeRight()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[1].x-=(int)((positionsSelectionRange[1].x-positionsSelectionRange[0].x)/2);
		}
		else
		{
			positionsSelectionRange[0].x-=(int)((positionsSelectionRange[0].x-positionsSelectionRange[1].x)/2);
		}
		singlePixelChange=true;
		repaint();
	}

//=======================================================
	public void doubleSelectionRangeLeft()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[0].x-=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
		}
		else
		{
			positionsSelectionRange[1].x-=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
		}
		singlePixelChange=true;
		repaint();
	}

//=======================================================
	public void halveSelectionRangeLeft()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[0].x+=(int)((positionsSelectionRange[1].x-positionsSelectionRange[0].x)/2);
		}
		else
		{
			positionsSelectionRange[0].x+=(int)((positionsSelectionRange[0].x-positionsSelectionRange[1].x)/2);
		}
		singlePixelChange=true;
		repaint();
	}

//=======================================================
	public void nudgeSelectionRangeLeft()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			int saveStart=positionsSelectionRange[0].x;
			positionsSelectionRange[0].x-=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
			positionsSelectionRange[1].x=saveStart;

			if(positionsSelectionRange[1].x<=m.scrollOffset)
			{
				m.scrollbar.setValue((int)(positionsSelectionRange[1].x-m.visibleRect.getWidth()));
			}
			else
			{
				singlePixelChange=true;
				repaint();
			}
		}
		else
		{
			int saveStart=positionsSelectionRange[1].x;
			positionsSelectionRange[1].x-=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
			positionsSelectionRange[0].x=saveStart;

			if(positionsSelectionRange[0].x<=m.scrollOffset)
			{
				m.scrollbar.setValue((int)(positionsSelectionRange[0].x-m.visibleRect.getWidth()));
			}
			else
			{
				singlePixelChange=true;
				repaint();
			}
		}
	}//end nudgeSelectionRangeLeft

//=======================================================
	public void nudgeSelectionRangeRight()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			int saveEnd=positionsSelectionRange[1].x;
			positionsSelectionRange[1].x+=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
			positionsSelectionRange[0].x=saveEnd;

			if(positionsSelectionRange[0].x>=m.scrollOffset+m.visibleRect.getWidth())
			{
				m.scrollbar.setValue((int)positionsSelectionRange[0].x);
			}
			else
			{
				singlePixelChange=true;
				repaint();
			}
		}
		else
		{
			int saveEnd=positionsSelectionRange[0].x;
			positionsSelectionRange[0].x+=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
			positionsSelectionRange[1].x=saveEnd;

			if(positionsSelectionRange[1].x>=m.scrollOffset+m.visibleRect.getWidth())
			{
				m.scrollbar.setValue(positionsSelectionRange[1].x);
			}
			else
			{
				singlePixelChange=true;
				repaint();
			}
		}
	}//nudgeSelectionRangeRight


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

//=======================================================
	public void paintComponent(Graphics g)
	{
		//sanity check
		if(m.props==null || !m.props.isValid() || m.scrollpane==null || m.visibleRect==null || m.visibleRect.getWidth()<1)
		{
			return;
		}
		if(suppressRepaint)
		{
			//m.p("repaint was suppressed");
			return;
		}

		//super.paintComponent(g);

		//remove all in viewport
		if(clearDue)
		{
			m.p("clear was due");
			g.clearRect(0,0,getWidth(),getHeight());
			clearDue=false;
		}

		//expect previously drawn image
		if(singlePixelChange || 
			(dragOngoing && 
				(dragType==DRAG_CREATE_SELECTION 
				|| dragType==DRAG_MOVE_SELECTION )))
		{
			//draw last buffered image here
			if(img!=null)
			{
				g.drawImage(img,m.scrollOffset,0,null);
			}

			final Graphics2D g2 = (Graphics2D) g;
			//mouse position / single pixel
			g2.setXORMode(Colors.wave_foreground);
			g2.setColor(Colors.wave_background);
			g2.fillRect(positions[1].x, 0,1, 1000);
			g2.setPaintMode();

			//marker
			g2.setColor(Colors.red);
			g2.fillRect((int)temporaryMarker, 0,1, 1000);

			singlePixelChange=false;
			//already done :)
			return;
		}

		//**************************
		//create array of waveblocks to display for viewport
		if(!scanDone)
		{
			//prevent java.util.ConcurrentModificationException
			//only needed if not finished scanning! increased memory use,
			copy = new ArrayList<AggregatedWaveBlock>(blocks);
		}
		else
		{
			copy=blocks;
		}

		if(drawFull) //i.e. to export as image (don't limit to viewport) (heavy)
		{
			use=copy;
		}
		else
		{
			int sublist_start=Math.min
			(
				m.scrollOffset*m.props.getChannels(),
				copy.size()
			);

			int sublist_end=Math.min
			(
				(int)(m.scrollOffset*m.props.getChannels()+m.visibleRect.getWidth() * m.props.getChannels()) ,
				copy.size()
			);
			//(further restrict sublist_start/end here)

			//test if use has changed
			if(blocks.size()>sublist_end 
				&& use!=null 
				&& use.size()==sublist_end-sublist_start 
				&& use.size()>0 
				&& use.get(0).block==blocks.get(sublist_start).block)
			{
				//System.out.print("u");
				//don't recopy, reuse
			}
			else
			{
				//System.out.print("c");
				//get sublist for current viewport
				use = new ArrayList<AggregatedWaveBlock>
				(
					copy.subList
					(
						sublist_start,sublist_end
					)
				);
			}
			//m.p("start "+sublist_start+" end "+sublist_end);
		}//end !drawFull

		//prevent the graph to "slip under" the scrollbar
		int sbHeight=0;
		if(m.scrollpane.getHorizontalScrollBar().isVisible())
		{
			sbHeight=m.scrollpane.getHorizontalScrollBar().getHeight();
		}

		//****
		//draw wave

		//draw all to buffered image, consider viewport size changes
		if(img==null 
			|| img.getWidth()!=(int)m.visibleRect.getWidth()
			|| img.getHeight()!=(int)m.visibleRect.getHeight()
		)
		{
			m.p("create new image");
			img=new BufferedImage(
				(int)m.visibleRect.getWidth(),
				(int)m.visibleRect.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		}

		//final Graphics2D g2=(Graphics2D)g;
		final Graphics2D g2=img.createGraphics();

		float waveHeightMax=(float) (( (m.scrollpane.getHeight()-sbHeight*2) /m.props.getChannels()) / 2);
		float waveHeight=waveHeightMax*0.95f;
		float baseLineY=0;

		AggregatedWaveBlock awb=null;
		AggregatedWaveBlock next=null;

		////
		g2.setBackground(new Color(255,255,255,0));
		g2.clearRect(0,0,img.getWidth(),img.getHeight());

		//*************************
		//main draw loop for blocks
		for(int i=0;i<use.size();i++)
		{
			awb=use.get(i);

			baseLineY=( (2*awb.channel-1) * waveHeightMax );

			//if high resolution == low sample per pixel value
			//==connect avg points of blocks
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
					g2.draw(new Line2D.Float(bl-m.scrollOffset, top, bl+1-m.scrollOffset, bottom));
				}
			}

			//==paint vertical amplitude blocks
			g2.setColor(Colors.wave_foreground);
			awb.paint(g2, waveHeight, baseLineY, -(float)m.scrollOffset);
		} //end for every block avg

///test selection locked to viewport
		g2.setXORMode(Colors.wave_foreground);
		g2.setColor(Color.yellow);
		g2.fillRect(100,0,400,2000);
		g2.setPaintMode();

		//free gfx resources of img Graphics
		//g2.dispose();

		//put created image to screen
		g.drawImage(img,m.scrollOffset,0,null);

		Graphics2D g2g=(Graphics2D)g;

		//draw over edit point (not in image)
		g2g.setColor(Colors.red);
		g2g.fillRect(temporaryMarker, 0,1, 1000);
	}//end new paintComponent

//=======================================================
	public void suppressRepaint(boolean b)
	{
		suppressRepaint=b;
	}

//=======================================================
	public void repaintWhileLoading(long updateCounter)
	{
		if(firstCounterWhileLoading==0)
		{
			firstCounterWhileLoading=updateCounter;
			repaint();
			return;
		}

		if(!waitWithRepaintWhileLoading && updateCounter>m.visibleRect.getWidth())
		{
			//m.p("updateCounter "+updateCounter+" "+m.visibleRect.getWidth());
			repaint();
			waitWithRepaintWhileLoading=true;
		}
	}

//=======================================================
	private class GraphViewport extends JViewport
	{
		public GraphViewport()
		{
			setOpaque(false);
		}

		@Override
		public void paintComponent(Graphics g)
		{
			//sanity check
			if(m.props==null || !m.props.isValid() || m.scrollpane==null || m.visibleRect==null || m.visibleRect.getWidth()<1)
			{
				return;
			}
			if(suppressRepaint)
			{
				//m.p("repaint was suppressed");
				return;
			}

			//super.paintComponent(g);

			//remove all in viewport
			if(clearDue)
			{
				m.p("clear was due");
				g.clearRect(0,0,getWidth(),getHeight());
				clearDue=false;
			}

			//prevent the graph to "slip under" the scrollbar
			int sbHeight=0;
			if(m.scrollpane.getHorizontalScrollBar().isVisible())
			{
				sbHeight=m.scrollpane.getHorizontalScrollBar().getHeight();
			}

			//channels should never be 0 (div zero!)
			float waveHeightMax=(float) (( (m.scrollpane.getHeight()-sbHeight*2) /m.props.getChannels()) / 2);
			float waveHeight=waveHeightMax*0.95f;
			float baseLineY=0;

			final Graphics2D g2 = (Graphics2D) g;

			//==scale under wave lanes
			g2.setStroke(stroke1);
			g2.setColor(Colors.canvas_grid);

			for(int r=(int)100* (int)(m.scrollOffset/100);r<m.scrollOffset+m.visibleRect.getWidth();r+=100)
			{
				g2.draw(new Line2D.Double(r-1-m.scrollOffset, 0, r-1-m.scrollOffset, 1000));
				g2.draw(new Line2D.Double(r+1-m.scrollOffset, 0, r+1-m.scrollOffset, 1000));
			}

			//==channel background
			final BasicStroke strokeChannelBackground = 
			new BasicStroke(2*waveHeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

			for(int w=0;w<m.props.getChannels();w++)
			{
				baseLineY=( (2*(w+1)-1) * waveHeightMax );
	
				g2.setStroke(strokeChannelBackground);
				g2.setColor(Colors.wave_background);
				g2.draw(new Line2D.Double(0, baseLineY, getWidth(), baseLineY));

///test "fixed" selection
				g2.setStroke(strokeChannelBackground);
				g2.setColor(Color.blue.darker());
				g2.draw(new Line2D.Double(100, baseLineY, 500, baseLineY));
				g2.setPaintMode();

//current range selection
				g2.setStroke(strokeChannelBackground);
				g2.setColor(new Color(255,180,10));
				g2.draw(new Line2D.Double(positionsSelectionRange[0].x-m.scrollOffset, baseLineY, positionsSelectionRange[1].x-m.scrollOffset, baseLineY));

				//don't draw wave limits at small height
				if(waveHeightMax>10)
				{
					//1
					g2.setStroke(stroke1);
					g2.setColor(Colors.wave_delimiter_top);
					g2.draw(new Line2D.Double(0, baseLineY-waveHeight, getWidth(), baseLineY-waveHeight));

					//-1
					g2.setStroke(stroke2);
					g2.setColor(Colors.wave_delimiter_bottom);
					g2.draw(new Line2D.Double(0, baseLineY+waveHeight, getWidth(), baseLineY+waveHeight));
				}

				//paint zero-line
				g2.setStroke(stroke1);
				g2.setColor(Colors.wave_zeroline);
				g2.draw(new Line2D.Double(0, baseLineY, getWidth(), baseLineY));

			}//end for channels

			//==scale over wave lanes
			g2.setStroke(stroke1);
			g2.setColor(Colors.canvas_grid.brighter());

			for(int r=(int)100* (int)(m.scrollOffset/100);r<m.scrollOffset+m.visibleRect.getWidth();r+=100)
			{
				g2.draw(new Line2D.Double(r-m.scrollOffset, 0, r-m.scrollOffset, 1000));
				g2.draw(new Line2D.Double(r-m.scrollOffset, 0, r-m.scrollOffset, 1000));
			}

		}//end paintComponent
	}//end GraphViewport

//=======================================================
	public void mouseMoved(MouseEvent e)
	{
		positions[1]=e.getPoint();

		if(!dragOngoing) //set pointer according to position
		{
			if(e.getPoint().y<m.visibleRect.getHeight()/2)
			{
				cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR); 
				setCursor(cursor);
			}
			else
			{
				cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); 
				setCursor(cursor);
			}
		}

		if(m.haveValidFile)
		{
			m.mousePositionInGraph.setText("Pos "+m.df.format(e.getPoint().x));

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					//invoke a bit later
					//mouse released event after drag end processed first to let fully full redraw
					singlePixelChange=true;
					repaint();
				}
			});
		}
	}

//=======================================================
	public void mouseDragged(MouseEvent e)
	{
		if(mousePressed) //left button
		{
			dragOngoing=true;

			positions[1]=e.getPoint();

			//if starting click of drag was in top halve
			if(positionsCanvasDrag[0].y<m.visibleRect.getHeight()/2)
			{
				dragType=DRAG_CREATE_SELECTION;
				positionsSelectionRange[0].x=positions[0].x;
				positionsSelectionRange[0].y=positions[0].y;
				positionsSelectionRange[1]=e.getPoint();
				repaint();
			}
			else//bottom halve
			{
				//move selection
				if(e.isShiftDown())
				{
					dragType=DRAG_MOVE_SELECTION;

					int diff=positions[1].x-positions[0].x-offsetToSelectionStart;
					int diff2=positionsSelectionRange[1].x-positionsSelectionRange[0].x;

					positionsSelectionRange[0].x=positions[0].x+diff;
					positionsSelectionRange[1].x=positionsSelectionRange[0].x+diff2;

					//update to compensate for selection move//!
					//if shift released but button still pressed, continue normal canvas drag
					positionsCanvasDrag[0].x=positionsSelectionRange[0].x+offsetToSelectionStart;
					positionsCanvasDrag[1].x=positionsSelectionRange[0].x+offsetToSelectionStart;
					repaint();
				}
				else //move canvas
				{
					dragType=DRAG_MOVE_CANVAS;

					positionsCanvasDrag[1]=e.getPoint();

					int diff=positionsCanvasDrag[1].x-positionsCanvasDrag[0].x;
					m.scrollbar.setValue(m.scrollbar.getValue()-diff);
				}

			}//end click in bottom halve

		}//end left button mouse pressed
	}//end mouseDragged

//=======================================================
	public void mousePressed(MouseEvent e)
	{
		//m.p("mouse pressed; # of clicks: "+ e.getClickCount())+" "+e);
		if(e.getButton()==e.BUTTON1)
		{
			mousePressed=true;

			offsetToSelectionStart=e.getPoint().x-positionsSelectionRange[0].x;

			positions[0]=e.getPoint();
			positions[1]=e.getPoint();

			positionsCanvasDrag[0]=e.getPoint();
			positionsCanvasDrag[1]=e.getPoint();

			if(e.getPoint().y<m.visibleRect.getHeight()/2)
			{
				cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR); 
				setCursor(cursor);
			}
			else
			{
				cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); 
				setCursor(cursor);
			}

			//could be start of drag or in-place single click, not sure yet
			//repaint();
		}
	}

//=======================================================
	public void mouseReleased(MouseEvent e)
	{
		//m.p("released; # of clicks: "+ e.getClickCount()+" "+e);
		if(e.getButton()==e.BUTTON1)
		{
			mousePressed=false;
			dragOngoing=false;

			positions[1]=e.getPoint();

			if(dragType==DRAG_CREATE_SELECTION)
			{
				positionsSelectionRange[1]=e.getPoint();
			}

			dragType=DRAG_MOVE_CANVAS;

			//click in place
			if(positions[0].x==positions[1].x)
			{
				temporaryMarker=positions[0].x;

				if(positions[0].y<m.visibleRect.getHeight()/2)
				{
					//m.p("click in place on upper halve");

					//trim left (absolute minimum)
					if(e.isShiftDown())
					{
						if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
						{
							positionsSelectionRange[0].x=positions[0].x;
						}
						else
						{
							positionsSelectionRange[1].x=positions[0].x;
						}
					}
					//trim right (absolute maximum)
					else if(isControlOrMetaDown(e))
					{
						if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
						{
							positionsSelectionRange[1].x=positions[0].x;
						}
						else
						{
							positionsSelectionRange[0].x=positions[0].x;
						}
					}
				}
				else//bottom halve
				{
					//m.p("click in place on lower halve");

					//align absolute minimum
					int diff=0;
					if(e.isShiftDown())
					{
						if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
						{
							diff=positions[0].x-positionsSelectionRange[0].x;
						}
						else
						{
							diff=positions[0].x-positionsSelectionRange[1].x;
						}
					}
					//align absolute maximum
					else if(isControlOrMetaDown(e))
					{
						if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
						{
							diff=positions[0].x-positionsSelectionRange[1].x;
						}
						else
						{
							diff=positions[0].x-positionsSelectionRange[0].x;
						}
					}

					positionsSelectionRange[0].x+=diff;
					positionsSelectionRange[1].x+=diff;
				}//end bottom halve
			}//end click in place

			if(e.getPoint().y<m.visibleRect.getHeight()/2)
			{
				cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR); 
				setCursor(cursor);
			}
			else
			{
				cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); 
				setCursor(cursor);
			}

			singlePixelChange=true;
			repaint();

		}//end BUTTON1
	}//end mouseRelased

//=======================================================
	public void mouseEntered(MouseEvent e)
	{
		//m.p("entered "+e);
		mouseInside=true;

		///
		m.mousePositionInGraph.setText("Pos "+m.df.format(e.getPoint().x));

		singlePixelChange=false;
		suppressRepaint=false;
		repaint();
	}

//=======================================================
	public void mouseExited(MouseEvent e)
	{
		//m.p("exited "+ e);
		mouseInside=false;
		m.mousePositionInGraph.setText("(Mouse outside)");
	}

//=======================================================
	public static boolean isControlOrMetaDown(MouseEvent e)
	{
		if(m.os.isMac())
		{
			return e.isMetaDown();
		}
		else
		{
			return e.isControlDown();
		}
	}

//=======================================================
	public void mouseClicked(MouseEvent e)
	{
		//m.p("clicked (# of clicks: "+ e.getClickCount() + ")"+ e);
	}

//=======================================================
	public void saveImage()
	{
		//dummy
	}
}//end class WaveGraph
