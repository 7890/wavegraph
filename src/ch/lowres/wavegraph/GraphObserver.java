//tb/150119

package ch.lowres.wavegraph;

import java.util.*;

//=======================================================
public class GraphObserver implements Observer
{
	private static Main m;

	private boolean scanFinished=false;
	private int updateCounter=0;

	private WaveGraph graph;

//=======================================================
	public GraphObserver(WaveGraph wg)
	{
		graph=wg;
		graph.addGraphObserver(this);
	}

//=======================================================
	public void update(int status)
	{
		if(status>=WaveGraph.SET_DISPLAY_MONO 
			&& status <=WaveGraph.SET_DISPLAY_WIDTH)
		{
			//m.p("display changed");
		}
		else if(status>=WaveGraph.SELECTION_CLEAR 
			&& status <=WaveGraph.SET_SELECTION)
		{
			//m.p("selection changed");
			m.updateSelectionLabel();
		}
		else if(status==WaveGraph.SET_EDIT_POINT)
		{
			//m.p("edit point changed");
			m.updateEditPointLabel();
		}
		else if(status>=WaveGraph.MOUSE_MOVED 
			&& status <=WaveGraph.MOUSE_EXITED)
		{
			//m.p("mouse action");
			if(status==WaveGraph.MOUSE_PRESSED_2)
			{
				m.processFile(m.getStringFromPrimaryX11Selection());
			}
			else if(status==WaveGraph.MOUSE_DRAG_SELECTION_CREATE
				|| status==WaveGraph.MOUSE_DRAG_SELECTION_MOVE)
			{
				m.updateSelectionLabel();
			}
			else if(status==WaveGraph.MOUSE_ENTERED 
				|| status==WaveGraph.MOUSE_MOVED)
			{
				m.updateMousePointLabel();

			}
			else if(status==WaveGraph.MOUSE_EXITED)
			{
				m.updateMousePointLabel("(Mouse Outside)");
			}
		}
		else if(status==WaveGraph.SCROLLBAR_ADJUSTMENT_CHANGE)
		{
			//m.p("scrollbar adjusted");
			m.updateViewportLabel();
		}
	}//end update

//=======================================================
	public void update(Observable o, Object arg)
	{
		//all static variables from Main must be initialized (gui elements, graph, scanner etc)
		//m.p(arg.toString());
		if(arg instanceof Long)
		{
			long val=(Long)arg;

			updateCounter++;

			//don't redraw on every update
			//depends on requested resolution / width
			if(updateCounter>graph.getGraphWidth()/100)
			{
				int percent = (int)(100 * val / graph.getGraphWidth());
				m.scanProgressLabel.setText(" |  "+percent+"% Scanned");

				m.infoPanelTop.repaint();
				graph.repaintWhileLoading(val);

				updateCounter=0;
			}
		}
		else if(arg instanceof Integer)
		{
			int val=(Integer)arg;
			if(val==WaveScanner.STARTED)
			{
				m.buttonAbort.setVisible(true);
				m.buttonAbort.setEnabled(true);
				m.p("scan started");
			}
			else if(val==WaveScanner.INITIALIZED)
			{
				m.updateViewportInfoLabel();
				m.updateViewportLabel();
			}
			else if(val==WaveScanner.DONE)
			{
				updateCounter=0;
				scanFinished=true;
				m.scanProgressLabel.setText(" |  100% Scanned");
				m.buttonAbort.setEnabled(false);
				m.p("scan done.");
			}
			else if(val==WaveScanner.ABORTED)
			{
				scanFinished=true;
				updateCounter=0;
				m.buttonAbort.setEnabled(false);
				m.p("scan aborted.");
			}
			else if(val==WaveScanner.EXCEPTION)
			{
				scanFinished=true;
				updateCounter=0;
				m.buttonAbort.setEnabled(false);
				m.p("scan exception.");
			}

			m.infoPanelTop.repaint();
			graph.repaint();
		}//end if update arg was Integer
	}//end update
}//end class GraphObserver
