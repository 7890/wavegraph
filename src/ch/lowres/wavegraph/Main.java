//tb/150119

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;

import javax.swing.*;
import javax.imageio.*;

import java.io.*;
import java.util.*;

import java.text.*;

import javax.swing.plaf.basic.BasicScrollBarUI;

//=======================================================
public class Main //implements Observer
{
	public final static String progName="Wavegraph";
	public final static String progHome="https://github.com/7890/wavegraph";
	public final static String progVersion="0.000c";

	public static JFrame mainframe=new JFrame();
	public static Image appIcon=createImageFromJar("/resources/images/wavegraph_icon.png");

	public static AppMenu applicationMenu;

	public static JScrollPane scrollpane=new JScrollPane();
	public static JScrollBar scrollbar;
	public static int scrollbarIncrement=16;
	public static int scrollOffset=0; //relative to start of pane / panel
	public static Rectangle visibleRect=new Rectangle(0,0,0,0); //absolute, viewport
	public static long lastScrollValue=0;

	public static JPanel infoPanel=new JPanel(new WrapLayout(WrapLayout.LEFT));

	public static JLabel genericInfoLabel=new JLabel("");
	public static JLabel durationLabel=new JLabel("");
	public static JLabel scanProgressLabel=new JLabel("");// |  0% Scanned");
	public static JButton buttonAbort=new JButton("Abort Scan")
	{
		//make the same height as labels
		public Dimension getPreferredSize()
		{
			return new Dimension(130,(int)new JLabel(" ").getPreferredSize().getHeight());
		}
	};

	public static JPanel infoPanelBottom=new JPanel(new WrapLayout(WrapLayout.LEFT));
	public static JLabel viewPortInfoLabel1=new JLabel("");

	public static JLabel viewPortInfoLabelPixelsWidth=new JLabel("");
	public static JLabel viewPortInfoLabelPixelsFrom=new JLabel("");
	public static JLabel viewPortInfoLabelPixelsTo=new JLabel("");

	public static JLabel viewPortInfoLabelTimeWidth=new JLabel("");
	public static JLabel viewPortInfoLabelTimeFrom=new JLabel("");
	public static JLabel viewPortInfoLabelTimeTo=new JLabel("");

	public static JLabel mousePositionInGraph=new JLabel("");

	public static int ctrlOrCmd=InputEvent.CTRL_MASK;
	public static OSTest os=new OSTest();

	//filedialog "recently used" doesn't work in jre ~< 8
	public static FileDialog openFileDialog=new FileDialog(mainframe, "Select RIFF Wave File to Graph", FileDialog.LOAD);

	//public static String lastFileOpenDirectory=System.getProperty("user.dir");
	public static String lastFileOpenDirectory=os.getHomeDir();
	public static String currentFile=null;
	public static boolean haveValidFile=false;

	public static AboutDialog about;

//graph
	public static long width=0;

	public static int windowWidth=0;
	public static int windowHeight=0;

	public static WaveGraph graph;
	public static WaveScanner scanner;//=new WaveScanner();
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
//	public Main(String file)
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
		return showOpenFileDialog(lastFileOpenDirectory, currentFile);
	}
//=======================================================
	public static String showOpenFileDialog(String baseDir, String file)
	{
		//filter shown files
		OpenFileFilter filter=new OpenFileFilter();
		filter.addExtension(".wav");
		filter.addExtension(".wavex");
		openFileDialog.setFilenameFilter(filter);

		openFileDialog.setDirectory(baseDir);
		if(file!=null)
		{
			openFileDialog.setFile(file);
		}

		openFileDialog.setVisible(true);

		if(openFileDialog.getFile() == null || openFileDialog.getDirectory() == null)
		{
			return null;
		}

		//remember for next open
		lastFileOpenDirectory=openFileDialog.getDirectory();

		return lastFileOpenDirectory+openFileDialog.getFile();
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
		viewPortInfoLabel1.setText("Open File via Menu or Drag & Drop in Window");

		viewPortInfoLabelPixelsWidth.setText("");
		viewPortInfoLabelPixelsFrom.setText("");
		viewPortInfoLabelPixelsTo.setText("");

		viewPortInfoLabelTimeWidth.setText("");
		viewPortInfoLabelTimeFrom.setText("");
		viewPortInfoLabelTimeTo.setText("");

		viewPortInfoLabelTimeWidth.setText("");

		mousePositionInGraph.setText("");
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
			scanner.abort();
			updateTimer.stop();
			graph.clear();
			System.gc();
			width=0;
			scrollbar.setValue(0);
			resetAllLabels();

			props=scanner.getProps(currentFile);
			if(!props.isValid())
			{
				currentFile=null;
				haveValidFile=false;
				return;
			}

			haveValidFile=true;

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
					scrollpane.revalidate();
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

		scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrollpane.setWheelScrollingEnabled(true);

		scrollbar=scrollpane.getHorizontalScrollBar();
		scrollbar.setUnitIncrement(scrollbarIncrement);
		scrollbar.setUI(new BasicScrollBarUI());

		infoPanel.add(genericInfoLabel);
		infoPanel.add(durationLabel);
		infoPanel.add(scanProgressLabel);
		infoPanel.add(buttonAbort);

		buttonAbort.setEnabled(false);

		infoPanelBottom.add(viewPortInfoLabel1);

		infoPanelBottom.add(viewPortInfoLabelPixelsWidth);
		infoPanelBottom.add(viewPortInfoLabelPixelsFrom);
		infoPanelBottom.add(viewPortInfoLabelPixelsTo);

		infoPanelBottom.add(viewPortInfoLabelTimeWidth);
		infoPanelBottom.add(viewPortInfoLabelTimeFrom);
		infoPanelBottom.add(viewPortInfoLabelTimeTo);

		infoPanelBottom.add(mousePositionInGraph);;

		infoPanel.setBackground(new Color(215,215,215));
		infoPanelBottom.setBackground(new Color(215,215,215));

		mainframe.add(infoPanel, BorderLayout.NORTH);
		mainframe.add(scrollpane, BorderLayout.CENTER);
		mainframe.add(infoPanelBottom, BorderLayout.SOUTH);

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

		scrollpane.setViewportView(graph);
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

		viewPortInfoLabelPixelsWidth.setText(
			"|  "+
				df3.format(
				(long)visibleRect.getWidth())
		);

		viewPortInfoLabelPixelsFrom.setText(
			"|  "+
				df3.format(scrollOffset
				)
		);

		viewPortInfoLabelPixelsTo.setText(
			"-- "+
				df3.format(scrollOffset+visibleRect.getWidth()
				)
		);

		viewPortInfoLabelTimeWidth.setText(
			"|  "
			+props.getDurationString( 
				(long)visibleRect.getWidth()*scanner.getBlockSize())
		);

		viewPortInfoLabelTimeFrom.setText(
			"|  "
			+props.getDurationString( 
				(long)(scrollOffset*scanner.getBlockSize()))
		);


		//show "real" end if scroller at max (or graph less wide than window)
		if(
			scrollOffset+visibleRect.getWidth()>=scrollbar.getMaximum())
		{
			viewPortInfoLabelTimeTo.setText(
				"-- "
				+props.getDurationString(
					(long)(scanner.getCycles()))
			);
		}
		else
		{

			viewPortInfoLabelTimeTo.setText(
				"-- "
				+props.getDurationString( 
					(long)((scrollOffset+visibleRect.getWidth())*scanner.getBlockSize()))
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
					graph.setVisible(false);

					updateTimer.setInitialDelay(200);
					updateTimer.restart();
				}
			}
		});

		//
		updateTimer.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(haveValidFile)
				{
					//p(".");
					scrollOffset=scrollbar.getValue();

					visibleRect=scrollpane.getViewport().getVisibleRect();

					updateViewportLabel();

					graph.suppressRepaint(false);
					graph.setVisible(true);
					scrollpane.repaint();
					updateTimer.stop();
				}
			}//end actionPerformed	
		});//end addActionListener to updateTimer

		addDnDSupport();
		addGlobalKeyListeners();
		scanner.addObserver(scanObserver);
		addScrollbarListener();
	}

//=======================================================
	private static void addScrollbarListener()
	{
		//scrollbar shouldn't be visible when no file is loaded
		scrollbar.addAdjustmentListener(new AdjustmentListener()
		{
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				if(graph==null || props==null || !props.isValid() || scanner==null)
				{
					return;
				}
				//scrollOffset=e.getValue();
				scrollOffset=scrollbar.getValue();
				visibleRect=scrollpane.getViewport().getVisibleRect();

				updateViewportLabel();

				//if adjustment is not currently ongoing
				if(!e.getValueIsAdjusting())
				{
					updateTimer.stop();
					graph.suppressRepaint(false);
					graph.setVisible(true);
					scrollpane.repaint();
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
					graph.suppressRepaint(true);
					graph.setVisible(false);
				}
			}//end adjustmentValueChanged
		});//end addAdjustmentListener
	}//end addScrollbarListener

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
					java.util.List<File> droppedFiles = 
						(java.util.List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

					for (File file : droppedFiles)
					{
						p("drag+drop event: "+file.getAbsolutePath());
						processFile(file.getAbsolutePath());
						mainframe.toFront();
						buttonAbort.requestFocus();
						//only first
						break;
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

		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		//scroll left
		KeyStroke keyLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0);
		actionMap.put(keyLeft, new AbstractAction("LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()-scrollbarIncrement);
			}
		});

		//scroll left fast
		KeyStroke keyCtrlLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,ctrlOrCmd);
		actionMap.put(keyCtrlLeft, new AbstractAction("CTRL_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()-scrollbarIncrement*4);
			}
		});

		//scroll left faster
		KeyStroke keyShiftLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftLeft, new AbstractAction("SHIFT_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()-scrollbarIncrement*16);
			}
		});

		//scroll left very fast
		KeyStroke keyCtrlShiftLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
			ctrlOrCmd+ActionEvent.SHIFT_MASK);
		actionMap.put(keyCtrlShiftLeft, new AbstractAction("CTRL_SHIFT_LEFT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()-scrollbarIncrement*64);
			}
		});


		//scroll right
		KeyStroke keyRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0);
		actionMap.put(keyRight, new AbstractAction("RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()+scrollbarIncrement);
			}
		});

		//scroll right fast
		KeyStroke keyCtrlRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,ctrlOrCmd);
		actionMap.put(keyCtrlRight, new AbstractAction("CTRL_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()+scrollbarIncrement*4);
			}
		});

		//scroll right faster
		KeyStroke keyShiftRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,ActionEvent.SHIFT_MASK);
		actionMap.put(keyShiftRight, new AbstractAction("SHIFT_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()+scrollbarIncrement*16);
			}
		});

		//scroll right very fast
		KeyStroke keyCtrlShiftRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
			ctrlOrCmd+ActionEvent.SHIFT_MASK);
		actionMap.put(keyCtrlShiftRight, new AbstractAction("CTRL_SHIFT_RIGHT") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getValue()+scrollbarIncrement*64);
			}
		});

		//scroll to start
		KeyStroke keyHome = KeyStroke.getKeyStroke(KeyEvent.VK_HOME,0);
		actionMap.put(keyHome, new AbstractAction("HOME") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(0);
			}
		});

		//scroll to end
		KeyStroke keyEnd = KeyStroke.getKeyStroke(KeyEvent.VK_END,0);
		actionMap.put(keyEnd, new AbstractAction("END") 
		{
			public void actionPerformed(ActionEvent e)
			{
				scrollbar.setValue(scrollbar.getMaximum());
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
