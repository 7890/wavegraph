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

//import javax.swing.plaf.basic.BasicScrollBarUI;

//=======================================================
public class Main //implements Observer
{
	public final static String progName="Wavegraph";
	public final static String progHome="https://github.com/7890/wavegraph";
	public final static String progVersion="0.000d";

	public static JFrame mainframe=new JFrame();
	public static Image appIcon=createImageFromJar("/resources/images/wavegraph_icon.png");

	public static AppMenu applicationMenu;

	public static JPanel infoPanel=new JPanel(new WrapLayout(WrapLayout.LEFT));
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

	public static JPanel infoPanelBottom=new JPanel(new WrapLayout(WrapLayout.RIGHT));
	public static JPanel infoPanelBottomMasterGroup=new JPanel(new FlowLayout(FlowLayout.LEFT));

	public static JPanel infoPanelBottomGroup0=new JPanel(new GridLayout(3,1));
	public static JLabel viewPortInfoLabel1=new JLabel("");
	public static JLabel mousePositionInGraph=new JLabel("");

	public static JPanel infoPanelBottomGroup1=new JPanel(new GridLayout(4,1));
	public static JLabel viewPortInfoLabelPixelsFrom=new JLabel("",JLabel.CENTER);
	public static JLabel viewPortInfoLabelPixelsTo=new JLabel("",JLabel.CENTER);
	public static JLabel viewPortInfoLabelPixelsWidth=new JLabel("",JLabel.CENTER);
	public static JLabel infoPanelBottomGroup1Caption=new JLabel("Viewport Pixels",JLabel.CENTER);

	public static JPanel infoPanelBottomGroup2=new JPanel(new GridLayout(4,1));
	public static JLabel viewPortInfoLabelTimeFrom=new JLabel("",JLabel.CENTER);
	public static JLabel viewPortInfoLabelTimeTo=new JLabel("",JLabel.CENTER);
	public static JLabel viewPortInfoLabelTimeWidth=new JLabel("",JLabel.CENTER);
	public static JLabel infoPanelBottomGroup2Caption=new JLabel("Viewport H:M:S",JLabel.CENTER);

	public static int ctrlOrCmd=InputEvent.CTRL_MASK;
	public static OSTest os=new OSTest();

	public static String currentFile=null;
	public static boolean haveValidFile=false;

	public static AboutDialog about;

//graph
	public static long width=0;

	public static int windowWidth=0;
	public static int windowHeight=0;

	public static WaveGraph graph;
	public static WaveScanner scanner;
	public static WaveProperties props;
	public static WaveScannerObserver scanObserver;

	//map containing all global key actions
	public static HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();

	public static DecimalFormat df = new DecimalFormat("#,###,###,##0");
	public static DecimalFormat df2 = new DecimalFormat("#,###,###,##0.00");
	public static DecimalFormat df3 = new DecimalFormat("#,000,000,000");

	public static javax.swing.Timer updateTimer=new javax.swing.Timer(-1,null);

//=======================================================
	public static void main(String[] args) //throws Exception
	{
		createShutDownHook();

		p("~~~");
		p("welcome to "+progName+"!");
		p("by Thomas Brand <tom@trellis.ch>");
		p(progHome);

		p("");
		p("command line argument: (1) file to load");
		p("if no argument is given, a file dialog will be presented.");

		p("");
		p("Build Info:");
		p(BuildInfo.get());
		p("");

		//any errors should be catched
		Main m=new Main();

		if(args.length>0)
		{
			p("using file or directory given on command line");
			m.processFile(args[0]);
		}
	}

//=======================================================
	public Main()
	{
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

		applicationMenu=new AppMenu(this);
		createGUI();
		addListeners();
		updateTimer.setInitialDelay(40);

		resetAllLabels();
		mainframe.show();

		//processFile(file);
	}//end constructor

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
		genericInfoLabel.setText("");
		durationLabel.setText("");
		scanProgressLabel.setText("(No File Loaded or unknown File Format)");
		buttonAbort.setVisible(false);

		//force bottom panel to have size as with labels
		viewPortInfoLabel1.setText("");//Open File via Menu or Drag & Drop in Window");
		mousePositionInGraph.setText("");

		viewPortInfoLabelPixelsFrom.setText("");
		viewPortInfoLabelPixelsTo.setText("");
		viewPortInfoLabelPixelsWidth.setText("");

		viewPortInfoLabelTimeFrom.setText("");
		viewPortInfoLabelTimeTo.setText("");
		viewPortInfoLabelTimeWidth.setText("");

		viewPortInfoLabelTimeWidth.setText("");
	}

//=======================================================
	public static void processFile(String file)
	{
		if(file==null || file.equals(""))
		{
			return;
		}

		try
		{
			if(new File(file).isDirectory())
			{
				String tmp=showOpenFileDialog(new File(file).getAbsolutePath(),null);

				mainframe.toFront();
				buttonAbort.requestFocus();

				if(tmp==null)
				{
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

			haveValidFile=false;
			infoPanelBottom.setVisible(false);

			scanner.abort();
			updateTimer.stop();
			graph.clear();
			System.gc();
			width=0;
			resetAllLabels();

			props=scanner.getProps(currentFile);
			if(!props.isValid())
			{
				currentFile=null;
				haveValidFile=false;
				return;
			}

			haveValidFile=true;
			infoPanelBottom.setVisible(true);

			updateGenericInfoLabel();
			updateViewportLabel();

			mainframe.setTitle(progName+" - "+currentFile);

			//some auto logic for now
			//target size for whole file: 4 x windowWidth
			//only natural / exact, >=1 FPP frames per pixel value possible
			width=windowWidth*128;

			//resolution greater than 1 sample per pixel missing
			if(width>props.getFrameCount())
			{
				width=props.getFrameCount();
			}

			//start work
			scanner.scanData(width);
			//scanner.scanData((long)(props.getFrameCount()/2),(long)(props.getFrameCount()/2),width);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					graph.revalidate();
				}
			});

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

		infoPanel.setOpaque(true);
		infoPanel.setBackground(Colors.infopanel_background);

		infoPanelBottom.setOpaque(true);
		infoPanelBottom.setBackground(Colors.infopanel_background);
		infoPanelBottom.setVisible(false);

		infoPanelBottomMasterGroup.setOpaque(false);
		infoPanelBottomGroup0.setOpaque(false);
		infoPanelBottomGroup0.setPreferredSize(new Dimension(200,70));

		infoPanelBottomGroup1.setPreferredSize(new Dimension(110,70));
		infoPanelBottomGroup1Caption.setOpaque(true);
		infoPanelBottomGroup1Caption.setBackground(Colors.labelgroup_background);
		infoPanelBottomGroup1Caption.setForeground(Colors.labelgroup_foreground);

		infoPanelBottomGroup2.setPreferredSize(new Dimension(110,70));
		infoPanelBottomGroup2Caption.setOpaque(true);
		infoPanelBottomGroup2Caption.setBackground(Colors.labelgroup_background);
		infoPanelBottomGroup2Caption.setForeground(Colors.labelgroup_foreground);

		JPanel spacer=new JPanel(new GridLayout(3,1));
		spacer.add(new JLabel(" "));
		spacer.add(new JLabel(" "));
		spacer.add(new JLabel(" "));
		spacer.setOpaque(false);

		infoPanel.add(genericInfoLabel);
		infoPanel.add(durationLabel);
		infoPanel.add(scanProgressLabel);
		infoPanel.add(buttonAbort);

		infoPanelBottomGroup0.add(viewPortInfoLabel1);
		infoPanelBottomGroup0.add(mousePositionInGraph);

		infoPanelBottom.add(infoPanelBottomGroup0);

		infoPanelBottomGroup1.add(viewPortInfoLabelPixelsFrom);
		infoPanelBottomGroup1.add(viewPortInfoLabelPixelsTo);
		infoPanelBottomGroup1.add(viewPortInfoLabelPixelsWidth);

		infoPanelBottomGroup1Caption.setFont(new JLabel().getFont().deriveFont(10f));
		infoPanelBottomGroup1.add(infoPanelBottomGroup1Caption);

		infoPanelBottomMasterGroup.add(spacer);
		infoPanelBottomMasterGroup.add(infoPanelBottomGroup1);

		infoPanelBottomGroup2.add(viewPortInfoLabelTimeFrom);
		infoPanelBottomGroup2.add(viewPortInfoLabelTimeTo);
		infoPanelBottomGroup2.add(viewPortInfoLabelTimeWidth);

		infoPanelBottomGroup2Caption.setFont(new JLabel().getFont().deriveFont(10f));
		infoPanelBottomGroup2.add(infoPanelBottomGroup2Caption);

		infoPanelBottomMasterGroup.add(spacer);
		infoPanelBottomMasterGroup.add(infoPanelBottomGroup2);

		infoPanelBottom.add(infoPanelBottomMasterGroup);

		Dimension screenDimension=Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(mainframe.getGraphicsConfiguration());

		windowWidth=(int)(screenDimension.getWidth()-insets.left-insets.right);
		windowHeight=(int)((screenDimension.getHeight()-insets.top-insets.bottom)/2);

		mainframe.setSize(windowWidth,windowHeight);

		about=new AboutDialog(mainframe, "About "+progName, true);

		//the main things to do 
		graph=new WaveGraph();
		scanner=new WaveScanner(graph);
		scanObserver=new WaveScannerObserver();

		mainframe.add(infoPanel, BorderLayout.NORTH);
		mainframe.add(graph, BorderLayout.CENTER);
		mainframe.add(infoPanelBottom, BorderLayout.SOUTH);

		//mainframe.pack();
		//mainframe.show();
	}//end createGUI

//========================================================================
	public static void updateGenericInfoLabel()
	{
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
			props.getBitsPerSample()+" bit "
			+props.getWaveFormat()+", "
			+props.getSampleRate()+" Hz, "
			+channelLabel+", "
			+df2.format((float)((double)props.getFileSize()/1000000))+" MB");

		durationLabel.setText(" |  "+props.getDurationString());
	}

//========================================================================
	public static void updateViewportLabel()
	{
		//p("update viewport label");

/*		viewPortInfoLabel2.setText(" |  Viewport: "+df.format(visibleRect.getWidth())
		+" / "+df.format(width)
		+" Pixels");
*/

		viewPortInfoLabelPixelsFrom.setText(
			"S: "+
				df3.format(graph.scrollOffset)
		);

		viewPortInfoLabelPixelsTo.setText(
			"E: "+
				df3.format(graph.scrollOffset+graph.visibleRect.getWidth())
		);

		viewPortInfoLabelPixelsWidth.setText(
			"L: "+
				df3.format(
				(long)graph.visibleRect.getWidth())
		);

		viewPortInfoLabelTimeFrom.setText(
			"S: "+
			props.getDurationString( 
				(long)(graph.scrollOffset*scanner.getBlockSize()))
		);

		viewPortInfoLabelTimeWidth.setText(
			"L: "+
			props.getDurationString( 
				(long)graph.visibleRect.getWidth()*scanner.getBlockSize())
		);


		//show "real" end if scroller at max (or graph less wide than window)
		if(
			graph.scrollOffset+graph.visibleRect.getWidth()>=graph.scrollbar.getMaximum())
		{
			viewPortInfoLabelTimeTo.setText(
				"E: "+
				props.getDurationString(
					(long)(scanner.getCycles()))
			);
		}
		else
		{

			viewPortInfoLabelTimeTo.setText(
				"E: "+
				props.getDurationString( 
					(long)((graph.scrollOffset+graph.visibleRect.getWidth())*scanner.getBlockSize()))
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
				scanner.abort();
			}
		});

		mainframe.addComponentListener(new ComponentAdapter() 
		{
			public void componentResized(ComponentEvent evt)
			{
				if(haveValidFile)
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
				if(haveValidFile)
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
		addGlobalKeyListeners();
		scanner.addObserver(scanObserver);
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
	private static void addGlobalKeyListeners()
	{
		JRootPane rootPane = mainframe.getRootPane();

		//to enable key repeat events in osx (lion, ..?) /!\:
		//defaults write -g ApplePressAndHoldEnabled -bool false

		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		//scroll left
		KeyStroke keyLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0);
		actionMap.put(keyLeft, new AbstractAction("LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollLeft(1);
			}
		});

		//scroll left fast
		KeyStroke keyCtrlLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,ctrlOrCmd);
		actionMap.put(keyCtrlLeft, new AbstractAction("CTRL_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollLeft(4);
			}
		});

		//scroll left faster
		KeyStroke keyShiftLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftLeft, new AbstractAction("SHIFT_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollLeft(16);
			}
		});

		//scroll left very fast
		KeyStroke keyCtrlShiftLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
			ctrlOrCmd+ActionEvent.SHIFT_MASK);
		actionMap.put(keyCtrlShiftLeft, new AbstractAction("CTRL_SHIFT_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollLeft(64);
			}
		});

		//scroll right
		KeyStroke keyRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0);
		actionMap.put(keyRight, new AbstractAction("RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollRight(1);
			}
		});

		//scroll right fast
		KeyStroke keyCtrlRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,ctrlOrCmd);
		actionMap.put(keyCtrlRight, new AbstractAction("CTRL_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollRight(4);
			}
		});

		//scroll right faster
		KeyStroke keyShiftRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftRight, new AbstractAction("SHIFT_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollRight(16);
			}
		});

		//scroll right very fast
		KeyStroke keyCtrlShiftRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
			ctrlOrCmd+ActionEvent.SHIFT_MASK);
		actionMap.put(keyCtrlShiftRight, new AbstractAction("CTRL_SHIFT_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.scrollRight(64);
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
				graph.doubleSelectionRangeRight();
			}
		});

		//'f' halve range right
		KeyStroke keyf = KeyStroke.getKeyStroke('f');
		actionMap.put(keyf, new AbstractAction("f") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.halveSelectionRangeRight();
			}
		});

		//'d' halve range left
		KeyStroke keyd = KeyStroke.getKeyStroke('d');
		actionMap.put(keyd, new AbstractAction("d") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.halveSelectionRangeLeft();
			}
		});

		//'s' double range left
		KeyStroke keys = KeyStroke.getKeyStroke('s');
		actionMap.put(keys, new AbstractAction("s") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.doubleSelectionRangeLeft();
			}
		});

		//'e' nudge range left
		KeyStroke keye = KeyStroke.getKeyStroke('e');
		actionMap.put(keye, new AbstractAction("e") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.nudgeSelectionRangeLeft();
			}
		});

		//'r' nudge range left
		KeyStroke keyr = KeyStroke.getKeyStroke('r');
		actionMap.put(keyr, new AbstractAction("r") 
		{
			public void actionPerformed(ActionEvent e)
			{
				graph.nudgeSelectionRangeRight();
			}
		});
		//http://stackoverflow.com/questions/100123/application-wide-keyboard-shortcut-java-swing
		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher( new KeyEventDispatcher() 
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
		});
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
