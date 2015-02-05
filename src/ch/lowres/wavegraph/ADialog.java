//tb/14x

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import javax.swing.border.*;

import java.util.*;

import java.net.*;

//========================================================================
public abstract class ADialog extends JDialog
{
	protected static Main g;

	private JPanel form;
	private JPanel spacer;

	private JButton button_close=new JButton("Close");

	private TransparentTextArea tta;

//========================================================================
	public ADialog(Frame f, String title, boolean modality) 
	{
		super(f,title,modality);

		createForm();
		addWindowListeners();
		addActionListeners();
	}

//========================================================================
	public JTextPane getTextPane()
	{
		return tta;
	}

//========================================================================
	public void createForm()
	{
		setIconImage(g.appIcon);
		setLayout(new BorderLayout());

		form=new JPanel();
		form.setLayout(new BorderLayout());

		add(form,BorderLayout.NORTH);

		tta=new TransparentTextArea();
		tta.setContentType("text/html");
		//tta.setFont(f.fontNormal);
		tta.setText("");
		tta.setEditable(false);

		form.add(tta,BorderLayout.NORTH);

		button_close.setFocusable(false);
		button_close.setMnemonic('C');

		form.add(button_close,BorderLayout.SOUTH);

		decorate();

		setResizable(false);
		updateText();

		//done in calling object
		//setVisible(true);
	}//end createForm

//========================================================================
	public void decorate(){setUndecorated(false);}

//========================================================================
	public void updateText()
	{
		tta.setText(getHtml());
		pack();
		g.setDialogCentered(this);
	}

//========================================================================
	public static String ahref(String url)
	{
		return "<a href=\""+url+"\">"+url+"</a>";
	}

//========================================================================
	public abstract String getHtml();

//========================================================================
	public void closeDialog()
	{
		setVisible(false);
		g.mainframe.toFront();
	}

//========================================================================
	public void addActionListeners()
	{
		button_close.addActionListener (new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				closeDialog();
			}
		});
	}

//========================================================================
	public class TransparentTextArea extends JTextPane
	{
		public TransparentTextArea()
		{
			setOpaque(false);
			setBorder(new CompoundBorder(new EmptyBorder(5,5,5,5), new LineBorder(Color.LIGHT_GRAY)));
			//setHighlighter(null);

			//http://stackoverflow.com/questions/9335604/java-change-font-in-a-jtextpane-containing-html
			putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			//setFont(f.fontNormal);

			//http://stackoverflow.com/questions/3693543/hyperlink-in-jeditorpane
			addHyperlinkListener(new HyperlinkListener()
			{
				public void hyperlinkUpdate(HyperlinkEvent e)
				{
					if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
					{
						openInBrowser(e.getURL().toString());
					}
				}
			});
		}

		//========================================================================
		//http://stackoverflow.com/questions/14364291/jpanel-gradient-background
		@Override
		protected void paintComponent(Graphics g)
		{
			//antialias
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
		}
	}//end TransparentTextArea

//========================================================================
	public void addWindowListeners()
	{
		addWindowListener(new WindowListener()
		{
			public void windowClosed(WindowEvent arg0) {closeDialog();}
			public void windowClosing(WindowEvent arg0) {closeDialog();}
			public void windowActivated(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
		});
	}//end addWindowListeners

//========================================================================
	//http://www.java2s.com/Tutorial/Java/0240__Swing/JDialogisspecifythatpressingtheEscapekeycancelsthedialo
	protected JRootPane createRootPane()
	{
		JRootPane rootPane = new JRootPane();

		KeyStroke keySpace = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0,false);
		KeyStroke keyEscape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0,false);
		KeyStroke keyCtrlD = KeyStroke.getKeyStroke(KeyEvent.VK_D,g.ctrlOrCmd,false);

		Action actionListenerClose = new AbstractAction()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				setVisible(false);
			}
		};

		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		//InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		inputMap.put(keySpace, "SPACE");
		rootPane.getActionMap().put("SPACE", actionListenerClose);

		inputMap.put(keyEscape, "ESCAPE");
		rootPane.getActionMap().put("ESCAPE", actionListenerClose);

		inputMap.put(keyCtrlD, "CTRL_D");
		rootPane.getActionMap().put("CTRL_D", actionListenerClose);

		return rootPane;
	}//end createRootPane

//========================================================================
	//http://stackoverflow.com/questions/19375091/how-to-open-url-by-desktop-object-in-java
	public static boolean openInBrowser(String url) 
	{
		Desktop desktop=Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if(desktop!=null && desktop.isSupported(Desktop.Action.BROWSE)) 
		{
			try 
			{
				desktop.browse(new URI(url));
				return true;
			}
			catch(Exception e) 
			{
				g.w("Could Not Browse URL");
				//e.printStackTrace();
			}
		}
		else
		{
			g.w("Could Not Browse URL");
		}
		return false;
	}//end openInBrowser
}//end class ADialog
