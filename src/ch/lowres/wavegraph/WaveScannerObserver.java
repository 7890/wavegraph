//tb/150119

package ch.lowres.wavegraph;

import java.util.*;

//=======================================================
public class WaveScannerObserver implements Observer
{
	private static Main m;

	private boolean scanFinished=false;
	private int updateCounter=0;

//=======================================================
	public void update(Observable o, Object arg)
	{
		//all static variables from Main must be initialized (gui elements, graph, scanner etc)
		//m.p(arg.toString());
		if(arg instanceof Long)
		{
			long val=(Long)arg;

			//if(val>1000+WaveScanner.DATA_AVAILABLE)
			//{
				//derive block no
				//val=val-WaveScanner.DATA_AVAILABLE-1000;

				updateCounter++;

				//don't redraw on every update
				//depends on requested resolution / width
				if(updateCounter>m.width/100)
				{
					int percent = (int)(100 * val / m.width);
					m.scanProgressLabel.setText(" |  "+percent+"% Scanned");

					m.infoPanel.repaint();
					m.graph.repaint();

					updateCounter=0;
				}
			//}
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
				m.width=((WaveScanner)o).getOutputWidth();

				m.viewPortInfoLabel1.setText(
					m.df.format(m.scanner.getBlockSize())+" FPP, "
					+m.df.format(m.width)
			                +" Pixels"
				);
				m.p("frames per pixel: "+m.scanner.getBlockSize()+", frames to scan: "+m.scanner.getCycles());

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

			m.infoPanel.repaint();
			m.graph.repaint();
		}//end if update arg was Integer
	}//end update
}//end class WaveScannerObserver
