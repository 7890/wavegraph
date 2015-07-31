//tb/150119

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;

import javax.swing.*;
import javax.imageio.*;

import java.io.*;
import java.util.*;

import java.text.*;

import java.awt.datatransfer.*;
import java.awt.Toolkit;

//=======================================================
public class Main //implements Observer
{
	public final static String progName="Wavegraph";
	public final static String progHome="https://github.com/7890/wavegraph";
	public final static String progVersion="0.000h";

	public static JFrame mainframe=new JFrame();
	public static Image appIcon=createImageFromJar("/resources/images/wavegraph_icon.png");

	public static AppMenu applicationMenu;

	//north
	public static JPanel infoPanelTop=new JPanel(new WrapLayout(WrapLayout.LEFT));
	public static JLabel genericInfoLabel=new JLabel("");
	public static JLabel durationLabel=new JLabel("");
	public static JLabel scanProgressLabel=new JLabel("");
	public static JButton buttonAbort=new JButton("Abort Scan")
	{
		//make the same height as labels
		public Dimension getPreferredSize()
		{
			return new Dimension(130,(int)new JLabel(" ").getPreferredSize().getHeight());
		}
	};

	//south
	public static JPanel infoPanelBottom=new JPanel(new WrapLayout(WrapLayout.RIGHT));

	public static JLabel viewPortInfoLabel1=new JLabel("");

	public static RangeBox rangebox_Mousepoint=new RangeBox("Mouse");

	public static RangeBox rangebox_Editpoint=new RangeBox("Editpoint");

	public static RangeBox rangebox_selFrames=new RangeBox("Selection Frames");
	public static RangeBox rangebox_selPixels=new RangeBox("Selection Pixels");
	public static RangeBox rangebox_selHMS=new RangeBox("Selection H:M:S");

	public static RangeBox rangebox_vpFrames=new RangeBox("Viewport Frames");
	public static RangeBox rangebox_vpPixels=new RangeBox("Viewport Pixels");
	public static RangeBox rangebox_vpHMS=new RangeBox("Viewport H:M:S");


	public static int ctrlOrCmd=InputEvent.CTRL_MASK;
	public static OSTest os=new OSTest();

	public static String currentFile=null;

	public static AboutDialog about;

	public static int windowWidth=0;
	public static int windowHeight=0;

	public static WaveGraph graph;
	public static AudioFormat props;
	public static GraphObserver graphObserver;

	//map containing all global key actions
	public static HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();

	public static DecimalFormat df = new DecimalFormat("#,###,###,##0");
	public static DecimalFormat df2 = new DecimalFormat("#,###,###,##0.00");
//	public static DecimalFormat df3 = new DecimalFormat("#,000,000,000");
	public static DecimalFormat df3 = new DecimalFormat("#,###,###,##0");

	public static javax.swing.Timer updateTimer=new javax.swing.Timer(-1,null);

///mimic to know the roll status
	public static boolean is_playing=false;

//=======================================================
	public Main()
	{
		init();
		mainframe.show();

///test drive playhead
		new Thread(new Runnable()
                {
                        public void run()
                        {
				Point p=new Point();
				while(true)
				{
					p=graph.getPlayheadPoint();

					if(is_playing && props!=null && props.isValid() && props.getFrameCount()>0)
					{
						graph.setPlayheadPoint(p); //repaint handled inside
						p.x+=1;
					}

					try{Thread.sleep(32);}catch(Exception e){}
				}
                        }
                }).start();


	}//end constructor

//=======================================================
	private static void init()
	{
		createShutDownHook();
		if(os.isMac())
		{
			ctrlOrCmd=InputEvent.META_MASK;
			Mac.init();
		}

		DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
		symbols.setGroupingSeparator('\'');
		df.setDecimalFormatSymbols(symbols);
		df2.setDecimalFormatSymbols(symbols);

		DecimalFormatSymbols symbols3 = df3.getDecimalFormatSymbols();
		symbols3.setGroupingSeparator(' ');
		df3.setDecimalFormatSymbols(symbols3);

		Fonts.init();

		applicationMenu=new AppMenu();
		applicationMenu.setNoFileLoaded();
		createGUI();
		addListeners();
		updateTimer.setInitialDelay(40);

		resetAllLabels();

		Fonts.change(mainframe);
		Fonts.change(about);
		about.updateText();
	}

//=======================================================
	public static String showOpenFileDialog()
	{
		return showOpenFileDialog(null, currentFile);
	}

//=======================================================
	public static String showOpenFileDialog(String baseDir, String file)
	{

		return AFileChooser.showOpenFileDialog(baseDir, file);
	}

//=======================================================
	public static void resetAllLabels()
	{
		mainframe.setTitle(progName);

		//force labels to become blank
		props=null;

		updateGenericInfoLabel();
		updateViewportInfoLabel();
		updateSelectionLabel();
		updateViewportLabel();
		updateEditPointLabel();
		updateMousePointLabel();

		buttonAbort.setVisible(false);
	}

//=======================================================
	public static void processFile(String file)
	{
//		updateTimer.stop();

		if(file==null || file.equals(""))//i.e. openfile dialog cancelled
		{
			//only clear if no current file around
			if(currentFile==null || currentFile.equals(""))
			{
				applicationMenu.setNoFileLoaded();
				graph.clear();
				resetAllLabels();
			}
			return;
		}

		graph.clear();
		resetAllLabels();

		try
		{
			if(new File(file).isDirectory())
			{
				String tmp=showOpenFileDialog(new File(file).getAbsolutePath(),null);

				mainframe.toFront();
				buttonAbort.requestFocus();

				if(tmp==null)
				{
					applicationMenu.setNoFileLoaded();
					return;
				}
				else
				{
					currentFile=new File(tmp).getAbsolutePath();
				}
			}
			else
			{
				currentFile=new File(file).getAbsolutePath();
			}

			props=graph.scanner.getProps(currentFile);
			if(!props.isValid())
			{
				currentFile=null;
				applicationMenu.setNoFileLoaded();

				boolean show=graph.isDisplayScrollbar();
				graph.setDisplayScrollbar(false);
				graph.setDisplayScrollbar(show);

				return;
			}

			applicationMenu.setFileLoaded();
			applicationMenu.addRecentFile(new File(currentFile));

			updateGenericInfoLabel();
			updateViewportInfoLabel();
			updateSelectionLabel();
			updateViewportLabel();
			updateEditPointLabel();
			updateMousePointLabel();

			mainframe.setTitle(progName+" - "+currentFile);

			//some auto logic for now
			//target size for whole file: 4 x windowWidth
			//only natural / exact, >=1 FPP frames per pixel value possible
			long graphWidth=windowWidth*128;

			//resolution greater than 1 sample per pixel missing
			if(graphWidth>props.getFrameCount())
			{
				graphWidth=props.getFrameCount();
			}

			//start work
			graph.scanner.scanData(graphWidth);
			//scanner.scanData((long)(props.getFrameCount()/2),(long)(props.getFrameCount()/2),graphWidth);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}//end processFile

//=======================================================
	private static void createGUI()
	{
		mainframe.setIconImage(appIcon);
		mainframe.setTitle(progName);
		mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainframe.setJMenuBar(applicationMenu);
		mainframe.setLayout(new BorderLayout());

		buttonAbort.setEnabled(false);

		infoPanelTop.setOpaque(true);
		infoPanelTop.setBackground(Colors.infopanel_background);

		infoPanelBottom.setOpaque(true);
		infoPanelBottom.setBackground(Colors.infopanel_background);

		//north
		infoPanelTop.add(genericInfoLabel);
		infoPanelTop.add(durationLabel);
		infoPanelTop.add(viewPortInfoLabel1);
		infoPanelTop.add(scanProgressLabel);
		infoPanelTop.add(buttonAbort);

		//south
		//mouse
		infoPanelBottom.add(rangebox_Mousepoint);

		//editpoint
		infoPanelBottom.add(rangebox_Editpoint);

		//selection
		rangebox_selFrames.setFormat(df3);
		infoPanelBottom.add(rangebox_selFrames);

		rangebox_selPixels.setFormat(df3);
		infoPanelBottom.add(rangebox_selPixels);

		infoPanelBottom.add(rangebox_selHMS);

		//viewport
		rangebox_vpFrames.setFormat(df3);
		infoPanelBottom.add(rangebox_vpFrames);

		rangebox_vpPixels.setFormat(df3);
		infoPanelBottom.add(rangebox_vpPixels);

		infoPanelBottom.add(rangebox_vpHMS);

		Dimension screenDimension=Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(mainframe.getGraphicsConfiguration());

		windowWidth=(int)(screenDimension.getWidth()-insets.left-insets.right);
		windowHeight=(int)((screenDimension.getHeight()-insets.top-insets.bottom)/2);

		mainframe.setSize(windowWidth,windowHeight);

		about=new AboutDialog(mainframe, "About "+progName, true);

		//the main things to do 
		graph=new WaveGraph();
		graphObserver=new GraphObserver(graph);

		mainframe.add(infoPanelTop, BorderLayout.NORTH);
		mainframe.add(graph, BorderLayout.CENTER);
		mainframe.add(infoPanelBottom, BorderLayout.SOUTH);

		//mainframe.pack();
		//mainframe.show();
	}//end createGUI

//========================================================================
	public static void updateGenericInfoLabel()
	{
		if(props==null || !props.isValid())
		{
			genericInfoLabel.setText("");
			durationLabel.setText("");
			scanProgressLabel.setText("(No File Loaded or unknown File Format)");
			return;
		}

		//p("update generic info label");

		String channelLabel="";
		if(props.getChannels()==1)
		{
			channelLabel="Mono (1)";
		}
		else if(props.getChannels()==2)
		{
			channelLabel="Stereo (2)";
		}
		else if(props.getChannels()>2)
		{
			channelLabel="Multichannel ("+props.getChannels()+")";
		}

		genericInfoLabel.setText(
			props.getFileTypeName()+" "
			+props.getBitsPerSample()+" bit "
			+props.getWaveFormat()+", "
			+props.getSampleRate()+" Hz, "
			+channelLabel+", "
			+df2.format((float)((double)props.getFileSize()/1000000))+" MB");

		durationLabel.setText(" |  "+props.getDurationString());
	}

//========================================================================
	public static void updateSelectionLabel()
	{
		if(props==null || !props.isValid())
		{
			rangebox_selFrames.blank();
			rangebox_selPixels.blank();
			rangebox_selHMS.blank();
			return;
		}

		Point[] sel=graph.getSelectionRange();

		rangebox_selFrames.setStart(sel[0].x*graph.scanner.getBlockSize());
		rangebox_selFrames.setEnd(sel[1].x*graph.scanner.getBlockSize());
		rangebox_selFrames.setLength(
			(sel[1].x-sel[0].x)
				*graph.scanner.getBlockSize());

		rangebox_selPixels.setStart(sel[0].x);
		rangebox_selPixels.setEnd(sel[1].x);
		rangebox_selPixels.setLength(sel[1].x-sel[0].x);

		rangebox_selHMS.setStart(
			"S: "+
			props.getDurationString( 
				(long)(sel[0].x*graph.scanner.getBlockSize()))
		);

		rangebox_selHMS.setEnd(
			"E: "+
			props.getDurationString( 
				(long)(sel[1].x*graph.scanner.getBlockSize()))
		);

		rangebox_selHMS.setLength(
			"L: "+
			props.getDurationString( 
				(long)(sel[1].x-sel[0].x)*graph.scanner.getBlockSize())
		);
	}//end updateSelectionLabel

//========================================================================
	public static void updateMousePointLabel(String label)
	{
		rangebox_Mousepoint.setStart(label);
		rangebox_Mousepoint.setEnd("");
		rangebox_Mousepoint.setLength("");
	}

//========================================================================
	public static void updateMousePointLabel()
	{
		if(props==null || !props.isValid())
		{
			rangebox_Mousepoint.blank();
			return;
		}

		Point m=graph.getMousePoint();

		//frames
		rangebox_Mousepoint.setStart("F:  "+padLeft(
			df3.format(m.x*graph.scanner.getBlockSize()),11)+" "
		);
		//pixels
		rangebox_Mousepoint.setEnd("P:  "+padLeft(
			df3.format(m.x),11)+" "
		);
		//hms
		rangebox_Mousepoint.setLength("H: "+
			props.getDurationString(
				(long)m.x*graph.scanner.getBlockSize())
		);
	}

//========================================================================
	public static void updateEditPointLabel()
	{
		if(props==null || !props.isValid())
		{
			rangebox_Editpoint.blank();
			return;
		}

		Point ep=graph.getEditPoint();

		//frames
		rangebox_Editpoint.setStart("F:  "+padLeft(
			df3.format(ep.x*graph.scanner.getBlockSize()),11)+" "
		);
		//pixels
		rangebox_Editpoint.setEnd("P:  "+padLeft(
			df3.format(ep.x),11)+" "
		);
		//hms
		rangebox_Editpoint.setLength("H: "+
			props.getDurationString(
				(long)ep.x*graph.scanner.getBlockSize())
		);
	}

//========================================================================
	public static void updateViewportInfoLabel()
	{
		if(props==null || !props.isValid())
		{
			viewPortInfoLabel1.setText("");
			return;
		}

		viewPortInfoLabel1.setText(" |  "+
			df.format(graph.scanner.getBlockSize())+" FPP, "
			+df.format(graph.getGraphWidth())
			+" Pixels"
		);
		p("frames per pixel: "+graph.scanner.getBlockSize()+", frames to scan: "+graph.scanner.getCycles());
	}

//========================================================================
	public static void updateViewportLabel()
	{
		if(props==null || !props.isValid())
		{
			rangebox_vpFrames.blank();
			rangebox_vpPixels.blank();
			rangebox_vpHMS.blank();
			return;
		}

		//p("update viewport label");

		rangebox_vpFrames.setStart(graph.scrollOffset*graph.scanner.getBlockSize());
		rangebox_vpFrames.setEnd((graph.scrollOffset+graph.visibleRect.getWidth())*graph.scanner.getBlockSize());
		rangebox_vpFrames.setLength(graph.visibleRect.getWidth()*graph.scanner.getBlockSize());

		rangebox_vpPixels.setStart(graph.scrollOffset);
		rangebox_vpPixels.setEnd(graph.scrollOffset+graph.visibleRect.getWidth());
		rangebox_vpPixels.setLength(graph.visibleRect.getWidth());

		rangebox_vpHMS.setStart(
			"S: "+
			props.getDurationString( 
				(long)(graph.scrollOffset*graph.scanner.getBlockSize()))
		);

		rangebox_vpHMS.setLength(
			"L: "+
			props.getDurationString( 
				(long)graph.visibleRect.getWidth()*graph.scanner.getBlockSize())
		);


		//show "real" end if scroller at max (or graph less wide than window)
		if(
			graph.scrollOffset+graph.visibleRect.getWidth()>=graph.scrollbar.getMaximum())
		{
			rangebox_vpHMS.setEnd(
				"E: "+
				props.getDurationString(
					(long)(graph.scanner.getCycles()))
			);
		}
		else
		{
			rangebox_vpHMS.setEnd(
				"E: "+
				props.getDurationString( 
					(long)((graph.scrollOffset+graph.visibleRect.getWidth())*graph.scanner.getBlockSize()))
			);
		}
	}//end updateViewportLabel

//========================================================================
	private static void addListeners()
	{
		buttonAbort.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				buttonAbort.setEnabled(false);
				graph.scanner.abort();
			}
		});

		mainframe.addComponentListener(new ComponentAdapter() 
		{
			public void componentResized(ComponentEvent evt)
			{
				if(props!=null && props.isValid())
				{
					Component c = (Component)evt.getSource();
					//p("resized");
					updateTimer.stop();

					graph.suppressRepaint(true);
					graph.panel.setVisible(false);

					updateTimer.setInitialDelay(200);
					updateTimer.restart();
				}
			}
		});

		updateTimer.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(props!=null && props.isValid())
				{
					//p("==timer action");
					graph.scrollOffset=graph.scrollbar.getValue();

					graph.visibleRect=graph.getViewport().getVisibleRect();

					updateViewportLabel();

					graph.suppressRepaint(false);
					graph.panel.setVisible(true);
					graph.panel.repaint();
					updateTimer.stop();

				}
			}//end actionPerformed	
		});//end addActionListener to updateTimer

		addDnDSupport();
		createGlobalKeyActions();
		addGlobalKeyListeners();
	}

//=======================================================
	private static void addDnDSupport()
	{
		//http://stackoverflow.com/questions/9669530/drag-and-drop-file-path-to-java-swing-jtextfield
		mainframe.setDropTarget(new DropTarget()
		{
			public synchronized void drop(DropTargetDropEvent evt)
			{
				try
				{
					evt.acceptDrop(DnDConstants.ACTION_COPY);

					//files, directory, etc
					if(evt.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor))
					{
						java.util.List<File> droppedFiles = 
							(java.util.List<File>)
								evt.getTransferable().getTransferData(
									DataFlavor.javaFileListFlavor);

						for (File file : droppedFiles)
						{
							p("drag+drop event: "+file.getAbsolutePath());
							evt.dropComplete(true);
							processFile(file.getAbsolutePath());
							mainframe.toFront();
							buttonAbort.requestFocus();
							//only first
							break;
						}
					}
					//if DataFlavor.javaFileListFlavor was not available
					//try with string
					else if(evt.getTransferable().isDataFlavorSupported(DataFlavor.stringFlavor))
					{
						String dndString=(String)
							evt.getTransferable().getTransferData(
								DataFlavor.stringFlavor);
						p("got this drag&drop string:"+dndString);
						String[] lines = dndString.split(System.getProperty("line.separator"));
						//ev. multiple lines, try rough for now
						processFile(lines[0].substring(lines[0].indexOf(
							"file://")+7,lines[0].length()).trim());
						mainframe.toFront();
						buttonAbort.requestFocus();
					}
					else
					{
						w("dataflavor of drag&drop event is not supported");
					}
				}catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});
	}

//========================================================================
	public static String getStringFromPrimaryX11Selection()
	{
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=44233
		java.awt.datatransfer.Clipboard cb
			=java.awt.Toolkit.getDefaultToolkit().getSystemSelection();

		if(cb==null)
		{
			w("could not get system clipboard");
		}
		else
		{
			java.awt.datatransfer.Transferable tfb=cb.getContents(null);
			if(!tfb.isDataFlavorSupported(DataFlavor.stringFlavor))
			{
				w("current X11 selection can not be formatted as string...");
			}
			else
			{
				String data=null;
				try
				{
					data=(String)tfb.getTransferData(DataFlavor.stringFlavor);
				}
				catch (Exception e)
				{
					w("could not convert X11 seleciton to a string.");
				}
				if(data!=null && data.length()>0)
				{
					p("X11 selection is: "+data);
					String[] lines=data.split(System.getProperty("line.separator"));
					return lines[0];
				}
			}
		}//else clipboard not null
		return null;
	}//end getStringFromPrimaryX11Selection

//========================================================================
//http://www.javapractices.com/topic/TopicAction.do?Id=82
	public static String getStringFromClipboard()
	{
		String result = null;
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		//odd: the Object param of getContents is not currently used
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText=(contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasTransferableText)
		{
			try
			{
				result=(String)contents.getTransferData(DataFlavor.stringFlavor);
				String[] lines=result.split(System.getProperty("line.separator"));
				result=lines[0];
			}
			catch (Exception e)
			{
				w("could not read string from clipboard.");
				//e.printStackTrace();
			}
		}
		return result;
	}// end getStringFromClipboard

//========================================================================
	private static void createGlobalKeyActions()
	{

/*
However you will still have problems because the default InputMap only receives the key events 
when it has focus and by default a JPanel is not focusable. So you have two options:

a) make the panel focusable:

panel.setFocusable( true );

b) use a different InputMap:

inputMap = panel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
*/

//InputMap inputMap = graph.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//ActionMap actionMap = graph.getActionMap();

/*
		//handled by scrollbar, allowing to use arrow left in menu
		//scroll left
		KeyStroke keyLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0);
		actionMap.put(keyLeft, new AbstractAction("LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollBackward(1);
			}
		});
*/

		//scroll left fast
		KeyStroke keyCtrlLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,ctrlOrCmd);
		actionMap.put(keyCtrlLeft, new AbstractAction("CTRL_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollBackward(4);
			}
		});

		//scroll left faster
		KeyStroke keyShiftLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftLeft, new AbstractAction("SHIFT_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollBackward(16);
			}
		});

		//scroll left very fast
		KeyStroke keyCtrlShiftLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
			ctrlOrCmd+ActionEvent.SHIFT_MASK);
		actionMap.put(keyCtrlShiftLeft, new AbstractAction("CTRL_SHIFT_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollBackward(64);
			}
		});
/*
		//handled by scrollbar, allowing to use arrow right in menu
		//scroll right
		KeyStroke keyRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0);
		actionMap.put(keyRight, new AbstractAction("RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollForward(1);
			}
		});

*/
		//scroll right fast
		KeyStroke keyCtrlRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,ctrlOrCmd);
		actionMap.put(keyCtrlRight, new AbstractAction("CTRL_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollForward(4);
			}
		});

		//scroll right faster
		KeyStroke keyShiftRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftRight, new AbstractAction("SHIFT_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollForward(16);
			}
		});

		//scroll right very fast
		KeyStroke keyCtrlShiftRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
			ctrlOrCmd+ActionEvent.SHIFT_MASK);
		actionMap.put(keyCtrlShiftRight, new AbstractAction("CTRL_SHIFT_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollForward(64);
			}
		});

		//scroll to start
		KeyStroke keyHome = KeyStroke.getKeyStroke(KeyEvent.VK_HOME,0);
		actionMap.put(keyHome, new AbstractAction("HOME") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollToStart();
			}
		});

		//scroll to end
		KeyStroke keyEnd = KeyStroke.getKeyStroke(KeyEvent.VK_END,0);
		actionMap.put(keyEnd, new AbstractAction("END") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollToEnd();
			}
		});

		//'g' double range right
		KeyStroke keyg = KeyStroke.getKeyStroke('g');
		actionMap.put(keyg, new AbstractAction("g") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.doubleSelectionRangeEnd();
			}
		});

		//'f' halve range right
		KeyStroke keyf = KeyStroke.getKeyStroke('f');
		actionMap.put(keyf, new AbstractAction("f") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.halveSelectionRangeEnd();
			}
		});

		//'d' halve range left
		KeyStroke keyd = KeyStroke.getKeyStroke('d');
		actionMap.put(keyd, new AbstractAction("d") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.halveSelectionRangeStart();
			}
		});

		//'s' double range left
		KeyStroke keys = KeyStroke.getKeyStroke('s');
		actionMap.put(keys, new AbstractAction("s") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.doubleSelectionRangeStart();
			}
		});

		//'e' nudge range left
		KeyStroke keye = KeyStroke.getKeyStroke('e');
		actionMap.put(keye, new AbstractAction("e") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.nudgeSelectionRangeBackward();
			}
		});

		//'r' nudge range left
		KeyStroke keyr = KeyStroke.getKeyStroke('r');
		actionMap.put(keyr, new AbstractAction("r") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.nudgeSelectionRangeForward();
			}
		});

		//'m'
		KeyStroke keym = KeyStroke.getKeyStroke('m');
		actionMap.put(keym, new AbstractAction("m") 
		{
			public void actionPerformed(ActionEvent e)
			{
				//test
			}
		});

		//shift insert uri from primary x11 selection
		KeyStroke keyShiftInsert = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftInsert, new AbstractAction("SHIFT_INSERT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				String clipboardString=getStringFromPrimaryX11Selection();
				if(clipboardString!=null && clipboardString.length()>0)
				{
					processFile(clipboardString);
				}
			}
		});

		//ctrl + v uri from clipboard
		KeyStroke keyCtrlV = KeyStroke.getKeyStroke(KeyEvent.VK_V,ctrlOrCmd);
		actionMap.put(keyCtrlV, new AbstractAction("CTRL_V") 
		{
			public void actionPerformed(ActionEvent e)
			{
				String clipboardString=getStringFromClipboard();
				if(clipboardString!=null && clipboardString.length()>0)
				{
					processFile(clipboardString);
				}
			}
		});

		//esc show menu
		KeyStroke keyEsc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
		actionMap.put(keyEsc, new AbstractAction("ESC") 
		{
			public void actionPerformed(ActionEvent e)
			{
				if(about.isVisible())
				{
					about.setVisible(false);
					mainframe.toFront();
				}
				else
				{
					showMenu(true);
				}
			}
		});
	}//end createGlobalKeyActions

//========================================================================
	final static KeyboardFocusManager kfm=KeyboardFocusManager.getCurrentKeyboardFocusManager();

	//http://stackoverflow.com/questions/100123/application-wide-keyboard-shortcut-java-swing
	final static KeyEventDispatcher ked=new KeyEventDispatcher() 
		{
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				if ( actionMap.containsKey(keyStroke) )
				{
					final Action a = actionMap.get(keyStroke);
					final ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), null );
					SwingUtilities.invokeLater( new Runnable()
					{
						public void run()
						{
							a.actionPerformed(ae);
						}
					} ); 
					return true;
				}
				return false;
			}
		};

//========================================================================
	public static void removeGlobalKeyListeners()
	{
		kfm.removeKeyEventDispatcher(ked);
	}

//========================================================================
	public static void addGlobalKeyListeners()
	{
		kfm.addKeyEventDispatcher(ked);

	}//end addGlobalKeyListeners

//========================================================================
	public static void setDialogCentered(Dialog d)
	{
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(d.getGraphicsConfiguration());
		Dimension screenDimension=Toolkit.getDefaultToolkit().getScreenSize();

		d.setLocation(
			(int)((screenDimension.getWidth()-insets.left-insets.right-d.getWidth()) / 2),
			(int)((screenDimension.getHeight()-insets.top-insets.bottom-d.getHeight()) / 2)
		);
	}

//========================================================================
	public static void setDecorated(boolean deco)
	{
		mainframe.setVisible(false);
		mainframe.dispose();
		try{
			if(deco)
			{
				mainframe.setUndecorated(false);
			}
			else
			{
				mainframe.setUndecorated(true);
			}
		}catch(Exception e){}
		mainframe.setVisible(true);
		mainframe.validate();
	}

//========================================================================
	public static void showMenu(boolean show)
	{
		if(show && mainframe.getJMenuBar()==null)
		{
			mainframe.setJMenuBar(applicationMenu);
		}
		else if(!show)
		{
			mainframe.setJMenuBar(null);
		}
		mainframe.validate();
		graph.forceRepaint();
	}

//========================================================================
	public static void showInfoTop(boolean show)
	{
		if(show && !infoPanelTop.isVisible())
		{
			infoPanelTop.setVisible(true);
		}
		else if(!show)
		{
			infoPanelTop.setVisible(false);
		}
		mainframe.validate();
		graph.forceRepaint();
	}

//========================================================================
	public static void showInfoBottom(boolean show)
	{
		if(show && !infoPanelBottom.isVisible())
		{
			infoPanelBottom.setVisible(true);
		}
		else if(!show)
		{
			infoPanelBottom.setVisible(false);
		}
		mainframe.validate();
		graph.forceRepaint();
	}

//========================================================================
	public static Image createImageFromJar(String imageUriInJar)
	{
		InputStream is;
		Image ii;
		try
		{
			is=Main.class.getResourceAsStream(imageUriInJar);
			ii=ImageIO.read(is);
			is.close();
		}
		catch(Exception e)
		{
			w("could not load built-in image. "+e.getMessage());
			return null;
		}
		return ii;
	}//end createImageFromJar

//=======================================================
	private static void createShutDownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			public void run()
			{
				w("shutdown signal received!");
				///do stuff / clean up
			}
		}));
	}//end createShutDownHook

//=======================================================
	//http://stackoverflow.com/questions/388461/how-can-i-pad-a-string-in-java
	public static String padRight(String s, int n)
	{
		return String.format("%1$-" + n + "s", s);
	}

//=======================================================
	public static String padLeft(String s, int n)
	{
		return String.format("%1$" + n + "s", s);
	}

//=======================================================
	public static void p(String s)
	{
		System.out.println(s);
	}

//=======================================================
	public static void w(String s)
	{
		System.out.println("/!\\ "+s);
	}
}//end class Main
