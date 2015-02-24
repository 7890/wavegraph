//tb/150119

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;
import javax.swing.event.*;

import java.io.*;
import java.util.*;

import javax.imageio.*;

import java.text.*;

import javax.swing.plaf.basic.BasicScrollBarUI;

/*
WaveGraph (is JScrollPane)
has
   -------GraphPanel---------
        |GraphViewport|

WaveScanner
   WaveProperties

GraphObserver
   -mouse events
   -changed (via method) events

*/

//=======================================================
public class WaveGraph extends JScrollPane implements MouseMotionListener, MouseListener
{
	private static Main m;

	//inner class
	public GraphPanel panel=new GraphPanel();

	//inner class
	private GraphViewport viewport = new GraphViewport();

	public JScrollBar scrollbar;
	public int defaultScrollbarHeight=20;
	public int scrollbarIncrement=16;
	public int scrollOffset=0; //relative to start of pane / panel
	public Rectangle visibleRect=new Rectangle(0,0,0,0); //absolute, viewport
	public long lastScrollValue=0;

	private ArrayList<AggregatedWaveBlock> blocks = new ArrayList<AggregatedWaveBlock>();
	private ArrayList<AggregatedWaveBlock> copy=new ArrayList<AggregatedWaveBlock>();
	private ArrayList<AggregatedWaveBlock> use=new ArrayList<AggregatedWaveBlock>();

	private float waveHeight;
	private float baseLineY;

//////////////////
	private boolean scanDone=false;
	private boolean clearDue=true;
	private boolean drawFull=false;
	private boolean suppressRepaint=false;

	//0: press 1: current range end (release) or mouse if still pressed, current mouse
	private MouseEvent[] positions=new MouseEvent[3];

	//0: start 1: end
	//end can be < start and vice versa, some operations use absolute start/end (manually)
	private Point[] positionsSelectionRange=new Point[2];

	private Point[] positionsSelectionMove=new Point[2];
	private Point[] positionsCanvasMove=new Point[2];

	private Point positionAtShiftChange=new Point(0,0);

	private Point editPoint=new Point(0,0);

	//offset of mouse press position to positionsSelection[0]
	private int offsetToSelectionStart=0;
	private int scrollOffsetAtPress=0;

	private final int DRAG_NONE=0;
	private final int DRAG_MOVE_CANVAS=1;
	private final int DRAG_CREATE_SELECTION=2;
	private final int DRAG_MOVE_SELECTION=3;
	private final int DRAG_SWITCH=4;

	private int dragType=DRAG_NONE;

	private boolean mouseInside=false;
	private boolean dragOngoing=false;
	private boolean mousePressed=false;

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

	private BufferedImage imgLeft;
	private BufferedImage imgRight;;

	//display options
	private boolean displayMono=false;
	private boolean displayRectified=false;
	private boolean displayGrid=true;
	private boolean displayZeroLine=true;
	private boolean displayFilled=false;
	private boolean displayLimits=true;
	private boolean displayGap=true;

	private boolean displayHorizontalScrollbar=true;

	private float waveHeightFactor=.95f;

	//test
	private JPopupMenu popup_menu;

	public WaveScanner scanner;

	public ArrayList<GraphObserver> graphObservers=new ArrayList<GraphObserver>();;

	//notification events
	public final static int SET_DISPLAY_MONO		=1000;
	public final static int SET_DISPLAY_RECTIFIED		=1001;
	public final static int SET_DISPLAY_GRID		=1002;
	public final static int SET_DISPLAY_FILLED		=1003;
	public final static int SET_DISPLAY_MIDDLE_LINE		=1004;
	public final static int SET_DISPLAY_SCROLLBAR		=1005;
	public final static int SET_DISPLAY_LIMITS		=1006;
	public final static int SET_DISPLAY_GAP			=1007;
	public final static int SET_DISPLAY_WIDTH		=1008;

	public final static int SELECTION_CLEAR			=1100;
	public final static int SELECTION_ALL_IN_VIEWPORT	=1101;
	public final static int SELECTION_TRIM_START		=1102;
	public final static int SELECTION_TRIM_END		=1103;
	public final static int SELECTION_ALIGN_START		=1104;
	public final static int SELECTION_ALIGN_END		=1105;
	public final static int SELECTION_DOUBLE_END		=1106;
	public final static int SELECTION_HALVE_END		=1107;
	public final static int SELECTION_DOUBLE_START		=1108;
	public final static int SELECTION_HALVE_START		=1109;
	public final static int SELECTION_NUDGE_BACKWARD	=1110;
	public final static int SELECTION_NUDGE_FORWARD		=1111;
	public final static int SET_SELECTION			=1112;

	public final static int SET_EDIT_POINT			=1200;

	public final static int MOUSE_MOVED			=1300;
	public final static int MOUSE_DRAG_CANVAS_MOVE		=1301;
	public final static int MOUSE_DRAG_SELECTION_CREATE	=1302;
	public final static int MOUSE_DRAG_SELECTION_MOVE	=1303;

	public final static int MOUSE_PRESSED_1			=1400;
	public final static int MOUSE_PRESSED_2			=1401;
	public final static int MOUSE_PRESSED_3			=1402;

	public final static int MOUSE_RELEASED_1		=1403;
	public final static int MOUSE_RELEASED_2		=1404;
	public final static int MOUSE_RELEASED_3		=1405;

	public final static int MOUSE_CLICKED_IN_PLACE_1	=1406;
	public final static int MOUSE_CLICKED_IN_PLACE_2	=1407;
	public final static int MOUSE_CLICKED_IN_PLACE_3	=1408;

	public final static int MOUSE_ENTERED			=1409;
	public final static int MOUSE_EXITED			=1410;

	public final static int SCROLLBAR_ADJUSTMENT_CHANGE	=1500;

//=======================================================
	public WaveGraph()
	{
		scanner=new WaveScanner(this);

		createGUI();
		resetPositions();

		addMouseListeners();
		addKeyStrokeActions();
		addScrollbarListener();
	}

//=======================================================
	public void addGraphObserver(GraphObserver go)
	{
		if(!graphObservers.contains(go))
		{
			graphObservers.add(go);
		}
		scanner.addObserver(go);
	}

//=======================================================
	private void notifyObservers(int status)
	{
		for(GraphObserver go : graphObservers)
		{
			//
			go.update(status);
		}
	}

//=======================================================
	public long getGraphWidth()
	{
		return scanner.getOutputWidth();
	}

//=======================================================
	public void setEditPoint(Point p)
	{
		////validity check here

		notifyObservers(SET_EDIT_POINT);
		editPoint=p;
	}

//=======================================================
	public void setSelectionRange()
	{
		///

		notifyObservers(SET_SELECTION);
	}


//=======================================================
	public Point[] getSelectionRange()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			return positionsSelectionRange;
		}
		else
		{
			Point[] p=new Point[2];
			p[0]=positionsSelectionRange[1];
			p[1]=positionsSelectionRange[0];
			return p;
		}
	}

//=======================================================
	public Point getMousePoint()
	{
		return positions[2].getPoint();
	}

//=======================================================
	public void addMouseListeners()
	{
		panel.addMouseListener(this);
		panel.addMouseMotionListener(this);
	}

//=======================================================
	public void removeMouseListeners()
	{
		panel.removeMouseListener(this);
		panel.removeMouseMotionListener(this);
		cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		setCursor(cursor);
	}

//=======================================================
	private void createGUI()
	{
		setBackground(Colors.wave_canvas_background);

		panel.setOpaque(false);

		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		setWheelScrollingEnabled(true);

		scrollbar=getHorizontalScrollBar();
		scrollbar.setUnitIncrement(scrollbarIncrement);
		scrollbar.setUI(new BasicScrollBarUI());

		scrollbar.setPreferredSize(new Dimension(4000,defaultScrollbarHeight));
		scrollbar.setSize(new Dimension(4000,defaultScrollbarHeight));

		viewport.setView(panel);
		setViewport(viewport);
	}

//=======================================================
	public void addBlock(AggregatedWaveBlock awb)
	{
		blocks.add(awb);
	}

//=======================================================
	public void resetPositions()
	{
		MouseEvent event=new MouseEvent(panel,0,0,0,0,0,0,0,0,false,MouseEvent.BUTTON1);
		positions[0]=event;
		positions[1]=event;
		positions[2]=event;

		positionsSelectionRange[0]=new Point(0,0);
		positionsSelectionRange[1]=new Point(0,0);

		positionsSelectionMove[0]=new Point(0,0);
		positionsSelectionMove[1]=new Point(0,0);

		positionsCanvasMove[0]=new Point(0,0);
		positionsCanvasMove[1]=new Point(0,0);

		positionAtShiftChange=new Point(0,0);

		editPoint=new Point(0,0);
	}

//=======================================================
	public void clear()
	{
		//p("graph cleared");
		scanner.abort();
		scanDone=false;
		clearDue=true;
		blocks.clear();
		copy.clear();
		use.clear();
		resetPositions();
		scrollbar.setValue(0);
		firstCounterWhileLoading=0;
		waitWithRepaintWhileLoading=false;

		notifyObservers(SELECTION_CLEAR);
		notifyObservers(SET_EDIT_POINT);

		System.gc();

		forceRepaint();
	}

//=======================================================
	public void forceRepaint()
	{
		validate();
		scrollOffset=scrollbar.getValue();
		visibleRect=getViewport().getVisibleRect();
		panel.validate();
		img=null;
		imgLeft=null;
		imgRight=null;
		singlePixelChange=false;
		panel.repaint();
	}

//=======================================================
	public void setDisplayMono(boolean mono)
	{
		displayMono=mono;
		img=null;
		notifyObservers(SET_DISPLAY_MONO);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayMono()
	{
		return displayMono;
	}

//=======================================================
	public void setDisplayRectified(boolean rectified)
	{
		displayRectified=rectified;
		img=null;
		notifyObservers(SET_DISPLAY_RECTIFIED);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayRectified()
	{
		return displayRectified;
	}

//=======================================================
	public void setDisplayGrid(boolean show)
	{
		displayGrid=show;
		img=null;
		notifyObservers(SET_DISPLAY_GRID);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayGrid()
	{
		return displayGrid;
	}

//=======================================================
	public void setDisplayFilled(boolean show)
	{
		displayFilled=show;
		img=null;
		notifyObservers(SET_DISPLAY_FILLED);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayFilled()
	{
		return displayFilled;
	}

//=======================================================
	public void setDisplayMiddleLine(boolean show)
	{
		displayZeroLine=show;
		img=null;
		notifyObservers(SET_DISPLAY_MIDDLE_LINE);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayMiddleLine()
	{
		return displayZeroLine;
	}

//=======================================================
	public void setDisplayScrollbar(boolean show)
	{
		displayHorizontalScrollbar=show;

		//don't hide, make small instead to keep mousewheel scroll events

		if(show)
		{
			scrollbar.setPreferredSize(new Dimension(4000,defaultScrollbarHeight));
			scrollbar.setSize(new Dimension(4000,20));
		}
		else
		{
			scrollbar.setPreferredSize(new Dimension(0,0));
			scrollbar.setSize(new Dimension(0,0));
		}

		notifyObservers(SET_DISPLAY_SCROLLBAR);
		forceRepaint();
	}

//=======================================================
	public boolean isDisplayScrollbar()
	{
		return displayHorizontalScrollbar;
	}

//=======================================================
	public void setDisplayLimits(boolean show)
	{
		displayLimits=show;
		img=null;
		notifyObservers(SET_DISPLAY_LIMITS);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayLimits()
	{
		return displayLimits;
	}

//=======================================================
	public void setDisplayGap(boolean show)
	{
		displayGap=show;
		img=null;
		notifyObservers(SET_DISPLAY_GAP);
		panel.repaint();
	}

//=======================================================
	public boolean isDisplayGap()
	{
		return displayGap;
	}

////all range ops need limitation to >=0 <=width values

//=======================================================
	public void clearSelectionRange()
	{
		positionsSelectionRange[0]=new Point(0,0);
		positionsSelectionRange[1]=new Point(0,0);
		notifyObservers(SELECTION_CLEAR);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void selectAllInViewport()
	{
		positionsSelectionRange[0].x=(int)scrollOffset;
		positionsSelectionRange[1].x=(int)(scrollOffset+visibleRect.getWidth());
		notifyObservers(SELECTION_ALL_IN_VIEWPORT);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void trimSelectionRangeStart()
	{
		//implicitely use edit point
		trimSelectionRangeStart(editPoint.x);
	}

//=======================================================
	public void trimSelectionRangeStart(int newStartX)
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[0].x=newStartX;
		}
		else
		{
			positionsSelectionRange[1].x=newStartX;
		}

		notifyObservers(SELECTION_TRIM_START);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void trimSelectionRangeEnd()
	{
		//implicitely use edit point
		trimSelectionRangeEnd(editPoint.x);
	}

//=======================================================
	public void trimSelectionRangeEnd(int newEndX)
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[1].x=newEndX;
		}
		else
		{
			positionsSelectionRange[0].x=newEndX;
		}
		notifyObservers(SELECTION_TRIM_END);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void alignSelectionRangeStart()
	{
		//implicitely use edit point
		alignSelectionRangeStart(editPoint.x);
	}

//=======================================================
	public void alignSelectionRangeStart(int alignStartX)
	{
		int diff=0;

		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			diff=alignStartX-positionsSelectionRange[0].x;
		}
		else
		{
			diff=alignStartX-positionsSelectionRange[1].x;
		}
		//set new selection range value
		positionsSelectionRange[0].x+=diff;
		positionsSelectionRange[1].x+=diff;
		notifyObservers(SELECTION_ALIGN_START);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void alignSelectionRangeEnd()
	{
		//implicitely use edit point
		alignSelectionRangeEnd(editPoint.x);
	}

//=======================================================
	public void alignSelectionRangeEnd(int alignEndX)
	{
		int diff=0;

		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			diff=alignEndX-positionsSelectionRange[1].x;
		}
		else
		{
			diff=alignEndX-positionsSelectionRange[0].x;
		}

		//set new selection range value
		positionsSelectionRange[0].x+=diff;
		positionsSelectionRange[1].x+=diff;
		notifyObservers(SELECTION_ALIGN_END);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void doubleSelectionRangeEnd()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[1].x+=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
		}
		else
		{
			positionsSelectionRange[0].x+=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
		}
		notifyObservers(SELECTION_DOUBLE_END);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void halveSelectionRangeEnd()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[1].x-=(int)((positionsSelectionRange[1].x-positionsSelectionRange[0].x)/2);
		}
		else
		{
			positionsSelectionRange[0].x-=(int)((positionsSelectionRange[0].x-positionsSelectionRange[1].x)/2);
		}
		notifyObservers(SELECTION_HALVE_END);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void doubleSelectionRangeStart()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[0].x-=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
		}
		else
		{
			positionsSelectionRange[1].x-=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
		}
		notifyObservers(SELECTION_DOUBLE_START);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void halveSelectionRangeStart()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			positionsSelectionRange[0].x+=(int)((positionsSelectionRange[1].x-positionsSelectionRange[0].x)/2);
		}
		else
		{
			positionsSelectionRange[1].x+=(int)((positionsSelectionRange[0].x-positionsSelectionRange[1].x)/2);
		}

		notifyObservers(SELECTION_HALVE_START);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void nudgeSelectionRangeBackward()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			int saveStart=positionsSelectionRange[0].x;
			positionsSelectionRange[0].x-=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
			positionsSelectionRange[1].x=saveStart;

			if(positionsSelectionRange[1].x<=scrollOffset)
			{
				scrollbar.setValue((int)(positionsSelectionRange[1].x-visibleRect.getWidth()));
			}
			else
			{
				singlePixelChange=true;
				panel.repaint();
			}
		}
		else
		{
			int saveStart=positionsSelectionRange[1].x;
			positionsSelectionRange[1].x-=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
			positionsSelectionRange[0].x=saveStart;

			if(positionsSelectionRange[0].x<=scrollOffset)
			{
				scrollbar.setValue((int)(positionsSelectionRange[0].x-visibleRect.getWidth()));
			}
			else
			{
				singlePixelChange=true;
				panel.repaint();
			}
		}
		notifyObservers(SELECTION_NUDGE_BACKWARD);
	}//end nudgeSelectionRangeBackward

//=======================================================
	public void nudgeSelectionRangeForward()
	{
		if(positionsSelectionRange[0].x<=positionsSelectionRange[1].x)
		{
			int saveEnd=positionsSelectionRange[1].x;
			positionsSelectionRange[1].x+=(positionsSelectionRange[1].x-positionsSelectionRange[0].x);
			positionsSelectionRange[0].x=saveEnd;

			if(positionsSelectionRange[0].x>=scrollOffset+visibleRect.getWidth())
			{
				scrollbar.setValue((int)positionsSelectionRange[0].x);
			}
			else
			{
				singlePixelChange=true;
				panel.repaint();
			}
		}
		else
		{
			int saveEnd=positionsSelectionRange[0].x;
			positionsSelectionRange[0].x+=(positionsSelectionRange[0].x-positionsSelectionRange[1].x);
			positionsSelectionRange[1].x=saveEnd;

			if(positionsSelectionRange[1].x>=scrollOffset+visibleRect.getWidth())
			{
				scrollbar.setValue(positionsSelectionRange[1].x);
			}
			else
			{
				singlePixelChange=true;
				panel.repaint();
			}
		}
		notifyObservers(SELECTION_NUDGE_FORWARD);
	}//nudgeSelectionRangeEnd

//=======================================================
	public void scrollToStart()
	{
		scrollbar.setValue(0);
	}

//=======================================================
	public void scrollToEnd()
	{
		scrollbar.setValue(scrollbar.getMaximum());
	}

//=======================================================
	public void scrollBackward(int incrementMultiplier)
	{
		scrollbar.setValue(scrollbar.getValue()-(incrementMultiplier*scrollbarIncrement));
	}

//=======================================================
	public void scrollForward(int incrementMultiplier)
	{
		scrollbar.setValue(scrollbar.getValue()+(incrementMultiplier*scrollbarIncrement));
	}

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
			panel.repaint();
			return;
		}

		if(!waitWithRepaintWhileLoading && updateCounter>visibleRect.getWidth())
		{
			//p("updateCounter "+updateCounter+" "+visibleRect.getWidth());
			panel.repaint();
			waitWithRepaintWhileLoading=true;
		}
	}

//=======================================================
	public BufferedImage updateBufferedImage(BufferedImage bi, int offset)
	{
		//draw all to buffered image, consider viewport size changes
		if(bi==null 
			|| bi.getWidth()!=(int)visibleRect.getWidth()
			|| bi.getHeight()!=(int)visibleRect.getHeight()
		)
		{
			//p("create new image");
			bi=new BufferedImage(
				(int)visibleRect.getWidth(),
				(int)visibleRect.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
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
				offset*scanner.getProps().getChannels(),
				copy.size()
			);

			int sublist_end=Math.min
			(
				(int)(offset*scanner.getProps().getChannels()+visibleRect.getWidth() * scanner.getProps().getChannels()) ,
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
			//p("start "+sublist_start+" end "+sublist_end);
		}//end !drawFull

		int bottomGap=0;
		if(displayGrid)
		{
			bottomGap=16;
		}

		//****
		//draw wave

		//final Graphics2D g2=(Graphics2D)g;
		final Graphics2D g2=bi.createGraphics();

		float waveHeightMax=0;

		if(!displayMono)
		{
			//channels should never be 0 (div zero!)
			waveHeightMax=(float) (( (visibleRect.getHeight()-bottomGap) /scanner.getProps().getChannels()) / 2);
		}
		else
		{
			waveHeightMax=(float) ( (visibleRect.getHeight()-bottomGap) / 2);
		}

		float waveHeight=waveHeightMax*(displayGap ? waveHeightFactor : 1);
		float baseLineY=0;

		AggregatedWaveBlock awb=null;
		AggregatedWaveBlock next=null;

		////
		g2.setBackground(new Color(255,255,255,0));
		g2.clearRect(0,0,bi.getWidth(),bi.getHeight());

		//*************************
		//main draw loop for blocks
		for(int i=0;i<use.size();i++)
		{
			awb=use.get(i);

			if(!displayMono)
			{
				baseLineY=( (2*awb.channel-1) * waveHeightMax );
			}
			else
			{
				baseLineY=waveHeightMax;
			}

			//if high resolution == low sample per pixel value
			//==connect avg points of blocks
			if(awb.samples<512 || displayRectified)
			{
				//look ahead as long as not last
				if(i<=use.size()-1-scanner.getProps().getChannels())
				{
					next=use.get(i+scanner.getProps().getChannels());

					final long bl=awb.block;
					g2.setColor(Colors.wave_foreground.brighter());

					if(!displayRectified)//traditional
					{
						final float top=baseLineY-awb.avg*waveHeight;
						final float bottom=baseLineY-next.avg*waveHeight;

						g2.draw(new Line2D.Float(bl-offset, top, bl+1-offset, bottom));
					}
					else//rectified
					{
						float absmax=Math.abs(awb.max);
						float absmin=Math.abs(awb.min);

						float amplitude=Math.max(absmax,absmin)*waveHeight*2;

						float below=baseLineY+waveHeight;
						float above=below-amplitude;
						float avg=above;

						absmax=Math.abs(next.max);
						absmin=Math.abs(next.min);

						amplitude=Math.max(absmax,absmin)*waveHeight*2;

						above=below-amplitude;
						float avg_next=above;

						g2.draw(new Line2D.Float(bl-offset, avg, bl+1-offset, avg_next));
					}
				}
			}

			//==paint vertical amplitude blocks
			g2.setColor(Colors.wave_foreground);
			awb.paint(g2, waveHeight, baseLineY, -(float)offset, displayRectified, displayFilled);
		} //end for every block avg

///test selection locked to viewport
/*
		g2.setXORMode(Colors.wave_foreground);
		g2.setColor(Color.yellow);
		g2.fillRect(100,0,400,2000);
		g2.setPaintMode();
*/

		//free gfx resources of img Graphics
		g2.dispose();

		return bi;

	}//end updateBufferedImage

//===========================================================================================
//Mouse Events===============================================================================
//===========================================================================================
	public void mouseMoved(MouseEvent e)
	{
		if(!scanner.getProps().isValid())
		{
			return;
		}

		//mouse moved implicitely means no button is pressed, no drag ongoing

		//remember current mouse position for methods not having access to event
		positions[2]=e;

		//set pointer according to position
		//(if drag is ongoing, keep the cursor icon as is)
		if(!dragOngoing)
		{
			if(e.getPoint().y<visibleRect.getHeight()/2)
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

		notifyObservers(MOUSE_MOVED);

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				//invoke a bit later
				//mouse released event after drag end processed first to let fully full redraw
				singlePixelChange=true;
				panel.repaint();
			}
		});
	}

//=======================================================
	public void mouseDragged(MouseEvent e)
	{
		if(!scanner.getProps().isValid())
		{
			return;
		}

		//mouse dragged implicitely means a button is pressed

		if(!mousePressed) //left button (set at press time)
		{
			return;
		}

		positions[1]=e;
		positions[2]=e;

		dragOngoing=true;

		//if starting click of drag was in top halve
		if(positions[0].getPoint().y<visibleRect.getHeight()/2)
		{
			//p("drag create selection (top halve)");
			dragType=DRAG_CREATE_SELECTION;
			positionsSelectionRange[0]=positions[0].getPoint();
			positionsSelectionRange[1]=e.getPoint();
			notifyObservers(MOUSE_DRAG_SELECTION_CREATE);
		}
		else//bottom halve
		{
			//move selection
			if(e.isShiftDown())
			{
				//p("drag move selection (shift down, bottom halve)");
				dragType=DRAG_MOVE_SELECTION;
				positionsSelectionMove[1]=e.getPoint();
				int diff=positionsSelectionMove[1].x-positionsSelectionMove[0].x-offsetToSelectionStart;
				int diff2=positionsSelectionRange[1].x-positionsSelectionRange[0].x;
				positionsSelectionRange[0].x=positionsSelectionMove[0].x+diff;
				positionsSelectionRange[1].x=positionsSelectionRange[0].x+diff2;
				notifyObservers(MOUSE_DRAG_SELECTION_MOVE);
			}
			else //move canvas
			{
				//p("drag move canvas");
				dragType=DRAG_MOVE_CANVAS;
				positionsCanvasMove[1]=e.getPoint();

				notifyObservers(MOUSE_DRAG_CANVAS_MOVE);
			}
		}//end click in bottom halve
		panel.repaint();
	}//end mouseDragged

//=======================================================
	public void mousePressed(MouseEvent e)
	{
		//p("mouse pressed; # of clicks: "+ e.getClickCount())+" "+e);

		if(!scanner.getProps().isValid())
		{
			return;
		}
		//right / context click test
		if(e.getButton()==e.BUTTON3)
		{
			notifyObservers(MOUSE_PRESSED_3);
			if(popup_menu==null)
			{
				popup_menu=new APopupMenu();
			}
			popup_menu.show(e.getComponent(), e.getX(), e.getY());
		}

		//middle click test -> try to load from primary x11 selection
		if(e.getButton()==e.BUTTON2)
		{
			notifyObservers(MOUSE_PRESSED_2);
			return;
		}

		//only consider left mouse button
		if(e.getButton()!=e.BUTTON1)
		{
			return;
		}
		notifyObservers(MOUSE_PRESSED_1);

		//mouse down: could be start of drag or start of click-in-place

		//reset all to current
		positions[0]=e;
		positions[1]=e;
		positions[2]=e;

		mousePressed=true;
		dragOngoing=false;

		offsetToSelectionStart=e.getPoint().x-positionsSelectionRange[0].x;

		//remember where the scrollbar (real viewport) was
		scrollOffsetAtPress=scrollOffset;

		//p("mouse pressed, offsetToSelectionStart: "+offsetToSelectionStart+", scrollOffsetAtPress: "+scrollOffsetAtPress);

		positionsCanvasMove[0]=e.getPoint();
		positionsCanvasMove[1]=e.getPoint();

		positionsSelectionMove[0]=e.getPoint();
		positionsSelectionMove[1]=e.getPoint();

		if(e.getPoint().y<visibleRect.getHeight()/2)
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

//=======================================================
	public void mouseReleased(MouseEvent e)
	{
		//p("released; # of clicks: "+ e.getClickCount()+" "+e);

		if(!scanner.getProps().isValid())
		{
			return;
		}

		//only consider left mouse button
		if(e.getButton()!=e.BUTTON1)
		{
			if(e.getButton()!=e.BUTTON2)
			{
				notifyObservers(MOUSE_RELEASED_2);
			}
			else if(e.getButton()!=e.BUTTON3)
			{
				notifyObservers(MOUSE_RELEASED_3);
			}
			return;
		}
		notifyObservers(MOUSE_RELEASED_1);

		//mouse up: could be end of drag or end of click-in-place

		positions[1]=e;

		mousePressed=false;
		dragOngoing=false;

		//p("mouse released, dragType: "+dragType+" (0:NONE, 1:MOVE_CANVAS, 2: CREATE_SELECTION 3: MOVE_SELECTION");

		//update cursor depending on location
		if(e.getPoint().y<visibleRect.getHeight()/2)
		{
			cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR); 
			setCursor(cursor);
		}
		else
		{
			cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); 
			setCursor(cursor);
		}


		if(dragType==DRAG_CREATE_SELECTION)//end of create
		{
		}
		else if(dragType==DRAG_MOVE_SELECTION)//end of move selection
		{
		}
		else if(dragType==DRAG_MOVE_CANVAS)//end of move canvas
		{
			//"realize" the "virtual" drag

			//reset images for recreation
			img=null;
			imgLeft=null;
			imgRight=null;

			dragType=DRAG_NONE;

			//set the real viewport (scrollbar) respecting the drag distance
			int diff=positionsCanvasMove[1].x-positionsCanvasMove[0].x;

			scrollbar.setValue(scrollbar.getValue()-diff);
			return;
		}

		dragType=DRAG_NONE;

		//check for click in place (can not be a drag)
		if(positions[0].getPoint().x==positions[1].getPoint().x)
		{
			notifyObservers(MOUSE_CLICKED_IN_PLACE_1);

			//set makrer
			setEditPoint(positions[0].getPoint());

			if(positions[0].getPoint().y<visibleRect.getHeight()/2)
			{
				//p("click in place on upper halve");

				//trim left (absolute minimum)
				if(e.isShiftDown())
				{
					trimSelectionRangeStart(positions[0].getPoint().x);
				}
				//trim right (absolute maximum)
				else if(isControlOrMetaDown(e))
				{
					trimSelectionRangeEnd(positions[0].getPoint().x);
				}
			}
			else//bottom halve
			{
				//p("click in place on lower halve");

				//align absolute minimum
				int diff=0;
				if(e.isShiftDown())
				{
					alignSelectionRangeStart(positions[0].getPoint().x);
				}
				//align absolute maximum
				else if(isControlOrMetaDown(e))
				{
					alignSelectionRangeEnd(positions[0].getPoint().x);
				}
			}//end bottom halve
		}//end click in place

		singlePixelChange=true;
		panel.repaint();
	}//end mouseRelased

//=======================================================
	public void mouseEntered(MouseEvent e)
	{
		if(!scanner.getProps().isValid())
		{
			return;
		}
		//p("mouse entered");//+e);
		mouseInside=true;
		notifyObservers(MOUSE_ENTERED);
		singlePixelChange=true;
		panel.repaint();
	}

//=======================================================
	public void mouseExited(MouseEvent e)
	{
		if(!scanner.getProps().isValid())
		{
			return;
		}

		//p("mouse exited");//+ e);
		mouseInside=false;
		//create dummy event, "park" mouse at 0,0
		MouseEvent event=new MouseEvent(panel,0,0,0,0,0,0,0,0,false,MouseEvent.BUTTON1);
		positions[2]=event;
		notifyObservers(MOUSE_EXITED);
		singlePixelChange=true;
		panel.repaint();
	}


/*
getKeyStroke(int keyCode, int modifiers, boolean onKeyRelease)
Returns a shared instance of a KeyStroke, given a numeric key code and a set of modifiers, 
specifying whether the key is activated when it is pressed or released.

JPanel cannot gain focus so cannot interact with KeyEvents. 
Using KeyBindings, you can map an Action to a KeyStroke even 
when a component doesn't have focus.
*/
//http://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html

//=======================================================
	private void addKeyStrokeActions()
	{
		Action shiftUp = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(!scanner.getProps().isValid())
				{
					return;
				}
				if(positions[2].getPoint().x==positionAtShiftChange.x)
				{
					//p("same place");
				}
				positionAtShiftChange=positions[2].getPoint();

				if(dragType==DRAG_MOVE_SELECTION || dragType==DRAG_SWITCH)
				{
					dragType=DRAG_SWITCH;

					positionsCanvasMove[0]=positions[2].getPoint();
					positionsCanvasMove[1]=positions[2].getPoint();
				}
			}
		};

		Action shiftDown = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(!scanner.getProps().isValid())
				{
					return;
				}
				if(positions[2].getPoint().x==positionAtShiftChange.x)
				{
					//p("same place");
				}
				positionAtShiftChange=positions[2].getPoint();

				if(dragType==DRAG_MOVE_CANVAS || dragType==DRAG_SWITCH)//end of move canvas
				{
					//reset images for recreation
					//img=null;
					imgLeft=null;
					imgRight=null;

					dragType=DRAG_SWITCH;

					int diff=positionsCanvasMove[1].x-positionsCanvasMove[0].x;

					//p("--set new scrollbar value in shiftDown");

					positionsCanvasMove[0]=positions[2].getPoint();
					positionsCanvasMove[1]=positions[2].getPoint();

					scrollOffsetAtPress=scrollbar.getValue()-diff;
					scrollbar.setValue(scrollbar.getValue()-diff);
				}
			}
		};

		//react on shift up
		getInputMap().put(KeyStroke.getKeyStroke(
			KeyEvent.VK_SHIFT, 0 , true), "shiftUp");

		//react on shift down
		getInputMap().put(KeyStroke.getKeyStroke(
			KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK , false), "shiftDown");

		getActionMap().put("shiftUp",shiftUp);
		getActionMap().put("shiftDown",shiftDown);
	}//end addKeyStrokeActions

//=======================================================
	private void addScrollbarListener()
	{
		//scrollbar shouldn't be visible when no file is loaded
		scrollbar.addAdjustmentListener(new AdjustmentListener()
		{
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				//p("adjustment change");

				if(!scanner.getProps().isValid())
				{
					return;
				}

				//force redraw
				img=null;

				//scrollOffset=e.getValue();
				scrollOffset=scrollbar.getValue();
				visibleRect=getViewport().getVisibleRect();

				notifyObservers(SCROLLBAR_ADJUSTMENT_CHANGE);

/*
//tmp off
				//if adjustment is not currently ongoing
				if(!e.getValueIsAdjusting())
				{

					updateTimer.stop();
					suppressRepaint(false);
					setVisible(true);
					repaint();
				}

				else//is adjusting
				{
					if(lastScrollValue!=e.getValue())
					{
						lastScrollValue=e.getValue();
						updateTimer.stop();
						updateTimer.setInitialDelay(40);
						updateTimer.restart();
					}
					suppressRepaint(true);
					setVisible(false);
				}

*/
			}//end adjustmentValueChanged
		});//end addAdjustmentListener
	}//end addScrollbarListener

//=======================================================
	public static boolean isControlOrMetaDown(MouseEvent e)
	{
		if(OSTest.isMac())
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
		//p("clicked (# of clicks: "+ e.getClickCount() + ")"+ e);
	}

//=======================================================
	public void saveImage()
	{
		//dummy
	}

//===========================================================================================
//===========================================================================================
//===========================================================================================
	public class GraphPanel extends JPanel
	{
		public GraphPanel()
		{
			setOpaque(false);
		}

//=======================================================
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(
				(int)scanner.getOutputWidth(),
				(int)visibleRect.getHeight()
			);
		}

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
		@Override
		public void paintComponent(Graphics g)
		{
			//sanity check
			if(!scanner.getProps().isValid() || visibleRect.getWidth()<1)

			{
				return;
			}
			if(suppressRepaint)
			{
				//p("repaint was suppressed");
				return;
			}

			//super.paintComponent(g);

			//remove all in viewport
			if(clearDue)
			{
				//p("clear was due");
				g.clearRect(0,0,this.getWidth(),this.getHeight());
				clearDue=false;
			}

			if(dragOngoing && dragType==DRAG_MOVE_CANVAS && img!=null)
			{
				//System.out.print(".");

				g.drawImage(img,scrollOffsetAtPress + positionsCanvasMove[1].x-positionsCanvasMove[0].x,0,null);

				//drag to the right -> [0] < [1] -> left image needed
				if(positionsCanvasMove[0].x<positionsCanvasMove[1].x)
				{
					if(imgLeft==null)
					{
						imgLeft=updateBufferedImage(imgLeft,(int)(scrollOffset-visibleRect.getWidth()));
					}
					g.drawImage(imgLeft,(int)(scrollOffsetAtPress-visibleRect.getWidth() + positionsCanvasMove[1].x-positionsCanvasMove[0].x),0,null);
				}

				//drag to the left -> [0] > [1] -> right image needed
				if(positionsCanvasMove[0].x>positionsCanvasMove[1].x)
				{
					if(imgRight==null)
					{
						imgRight=updateBufferedImage(imgRight,(int)(scrollOffset+visibleRect.getWidth()));
					}
					g.drawImage(imgRight,(int)(scrollOffsetAtPress+visibleRect.getWidth() + positionsCanvasMove[1].x-positionsCanvasMove[0].x),0,null);	
				}


				final Graphics2D g2 = (Graphics2D) g;
				//mouse position / single pixel
				g2.setXORMode(Colors.wave_foreground);
				g2.setColor(Colors.wave_background);
				g2.fillRect(positions[2].getPoint().x, 0,1, 1000);
				g2.setPaintMode();

				//marker
				g2.setColor(Colors.red);
				g2.fillRect((int)editPoint.x+(positionsCanvasMove[1].x-positionsCanvasMove[0].x), 0,1, 1000);

				singlePixelChange=false;
				return;
			}

			//expect previously drawn image
			if(img!=null && (singlePixelChange || 
						(dragOngoing && 
							(dragType==DRAG_CREATE_SELECTION 
							|| dragType==DRAG_MOVE_SELECTION ))))
			{
				//System.out.print(";");

				//draw last buffered image here
				g.drawImage(img,scrollOffset,0,null);

				final Graphics2D g2 = (Graphics2D) g;
				//mouse position / single pixel
				if(mouseInside)
				{
					g2.setXORMode(Colors.wave_foreground);
					g2.setColor(Colors.wave_background);
					g2.fillRect(positions[2].getPoint().x, 0,1, 1000);

					g2.setPaintMode();
				}

				//marker
				g2.setColor(Colors.red);
				g2.fillRect((int)editPoint.x, 0,1, 1000);

				singlePixelChange=false;
				//already done :)

				return;
			}

			//p("/ "+scrollOffset+" "+scrollbar.getValue()+" "+scrollOffsetAtPress);

			//create new image from aggregated wave blocks
			img=updateBufferedImage(img,scrollOffset);

			//put created image to screen
			g.drawImage(img,scrollOffset,0,null);

			//try{p("start sleep ");Thread.sleep(2000); p("stop sleep\n\n\n");}catch(Exception e){}

			Graphics2D g2g=(Graphics2D)g;

			//draw over edit point (not in image)
			g2g.setColor(Colors.red);
			g2g.fillRect(editPoint.x, 0,1, 1000);

			//mouse will be drawn over as soon as moved
		}//end new paintComponent
	}//end class GraphPanel

//===========================================================================================
//===========================================================================================
//===========================================================================================
	private class GraphViewport extends JViewport
	{
//=======================================================
		public GraphViewport()
		{
			setOpaque(false);
		}

//=======================================================
		private void drawSelectionRange(Graphics2D gfx, int offset, float baseLineY, BasicStroke stroke)
		{
			//current range selection
			gfx.setStroke(stroke);
			gfx.setColor(new Color(255,180,10));
			gfx.draw(new Line2D.Double(positionsSelectionRange[0].x-offset, baseLineY, positionsSelectionRange[1].x-offset, baseLineY));

		}

//=======================================================
		private void drawScaleUnderWavelane(Graphics2D gfx, int offset)
		{
			if(!displayGrid)
			{
				return;
			}
			//==scale under wave lanes
			gfx.setStroke(stroke1);
			gfx.setColor(Colors.canvas_grid);

			for(int r=(int)100* (int)(offset/100);r<offset+visibleRect.getWidth();r+=100)
			{
				gfx.draw(new Line2D.Double(r-1-offset, 0, r-1-offset, 1000));
				gfx.draw(new Line2D.Double(r+1-offset, 0, r+1-offset, 1000));
			}
		}

//=======================================================
		private void drawScaleOverWavelane(Graphics2D gfx, int offset)
		{
			if(!displayGrid)
			{
				return;
			}
			//==scale over wave lanes
			gfx.setStroke(stroke1);
			gfx.setColor(Colors.canvas_grid.brighter());

			for(int r=(int)100* (int)(offset/100);r<offset+visibleRect.getWidth();r+=100)
			{
				gfx.draw(new Line2D.Double(r-offset, 0, r-offset, 1000));
				gfx.draw(new Line2D.Double(r-offset, 0, r-offset, 1000));
			}
		}

//=======================================================
		@Override
		public void paintComponent(Graphics g)
		{
			//sanity check
			if(!scanner.getProps().isValid() || visibleRect.getWidth()<1)
			{
				return;
			}
			if(suppressRepaint)
			{
				//p("repaint was suppressed");
				return;
			}

			//super.paintComponent(g);

			//remove all in viewport
			if(clearDue)
			{
				//p("clear was due");
				g.clearRect(0,0,this.getWidth(),this.getHeight());
				clearDue=false;
			}

			int bottomGap=0;
			if(displayGrid)
			{
				bottomGap=16;
			}

			float waveHeightMax=0;

			if(!displayMono)
			{
				//channels should never be 0 (div zero!)
				waveHeightMax=(float) (( (visibleRect.getHeight()-bottomGap) /scanner.getProps().getChannels()) / 2);
			}
			else
			{
				waveHeightMax=(float) ( (visibleRect.getHeight()-bottomGap) / 2);
			}

			float waveHeight=waveHeightMax*(displayGap ? waveHeightFactor : 1);
			float baseLineY=0;

			final Graphics2D g2 = (Graphics2D) g;

			if(dragOngoing && dragType==DRAG_MOVE_CANVAS)
			{
				drawScaleUnderWavelane(g2, scrollOffset-(positionsCanvasMove[1].x-positionsCanvasMove[0].x));
			}
			else//no drag ongoing or not move canvas drag mode
			{
				drawScaleUnderWavelane(g2, scrollOffset);
			}

			//==channel background
			final BasicStroke strokeChannelBackground = 
			new BasicStroke(2*waveHeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

			int displayChannelCount=0;

			if(!displayMono)
			{
				displayChannelCount=scanner.getProps().getChannels();
			}
			else
			{
				displayChannelCount=1;
			}

			//for(int w=0;w<scanner.getProps().getChannels();w++)
			for(int w=0;w<displayChannelCount;w++)
			{
				baseLineY=( (2*(w+1)-1) * waveHeightMax );
	
				g2.setStroke(strokeChannelBackground);
				g2.setColor(Colors.wave_background);
				g2.draw(new Line2D.Double(0, baseLineY, getWidth(), baseLineY));
/*
///test "fixed" selection
				g2.setStroke(strokeChannelBackground);
				g2.setColor(Color.blue.darker());
				g2.draw(new Line2D.Double(100, baseLineY, 500, baseLineY));
				g2.setPaintMode();
///current range selection
				g2.setStroke(strokeChannelBackground);
				g2.setColor(new Color(255,180,10));
				g2.draw(new Line2D.Double(positionsSelectionRange[0].x-scrollOffset, baseLineY, positionsSelectionRange[1].x-scrollOffset, baseLineY));
*/
				if(dragOngoing && dragType==DRAG_MOVE_CANVAS)
				{
					drawSelectionRange(g2, 
						scrollOffset-(positionsCanvasMove[1].x-positionsCanvasMove[0].x), 
						baseLineY, strokeChannelBackground);
				}
				else//no drag ongoing or not move canvas drag mode
				{
					drawSelectionRange(g2, scrollOffset, baseLineY, strokeChannelBackground);
				}

				//don't draw wave limits at small height or if turned off
				if(waveHeightMax>10 && displayLimits)
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
				if(displayZeroLine)
				{
					g2.setStroke(stroke1);
					g2.setColor(Colors.wave_zeroline);
					g2.draw(new Line2D.Double(0, baseLineY, getWidth(), baseLineY));
				}
			}//end for channels

			if(dragOngoing && dragType==DRAG_MOVE_CANVAS)
			{
				drawScaleOverWavelane(g2,scrollOffset-(positionsCanvasMove[1].x-positionsCanvasMove[0].x));
			}
			else//no drag ongoing or not move canvas drag mode
			{
				drawScaleOverWavelane(g2, scrollOffset);
			}
		}//end paintComponent
	}//end class GraphViewport
//=======================================================
	public class APopupMenu extends JPopupMenu implements PopupMenuListener
	{
		private JMenu menu;
		//private static Main m;
		public APopupMenu()
		{
			add(new JMenuItem("First Entry"));
			add(new JButton("test me"));
			addPopupMenuListener(this);
		}

		public void popupMenuWillBecomeVisible(PopupMenuEvent e)
		{
			//p("will become visible");
		}

		public void popupMenuCanceled(PopupMenuEvent e)
		{
			//p("popup cancelled");
		}

		public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
		{
			//p("will become invisible");
		}
	}//end class APopupMenu


//=======================================================
	public void p(String s)
	{
		System.out.println(s);
	}

}//end class WaveGraph
