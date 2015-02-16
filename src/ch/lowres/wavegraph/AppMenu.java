//tb/150122

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class AppMenu extends JMenuBar
{
	private static JMenu menu_main=new JMenu("File");

	private static JMenuItem mi_open_file=new JMenuItem("Open...");
	private static JMenuItem mi_save_image=new JMenuItem("Save As Image...");
	private static JMenuItem mi_quit=new JMenuItem("Quit");

	private static JMenu menu_view=new JMenu("View");
	private static JMenuItem mi_mono=new JMenuItem("Mono (Overlayed)");// vs. Multichannel
	private static JMenuItem mi_rectified=new JMenuItem("Rectified"); //vs. Normal
	private static JMenuItem mi_grid=new JMenuItem("Hide Grid"); //vs. Show Grid
	private static JMenuItem mi_middleLine=new JMenuItem("Hide Middle Line"); //vs. Show Middle Line

	private static JMenu menu_help=new JMenu("Help");
	private static JMenuItem mi_about=new JMenuItem("About...");

	private Main m;

//=======================================================
	public AppMenu(Main m)
	{
		this.m=m;

		createMenu();
		addActionListeners();
	}

//=======================================================
	private void createMenu()
	{
		mi_open_file.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_O, m.ctrlOrCmd));

		mi_save_image.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_S, m.ctrlOrCmd));

		mi_quit.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_Q, m.ctrlOrCmd));

		menu_main.add(mi_open_file);
//		menu_main.add(mi_save_image);
		menu_main.add(new JSeparator());
		menu_main.add(mi_quit);
		menu_main.setMnemonic('F');
		add(menu_main);

		menu_view.add(mi_mono);
		menu_view.add(mi_rectified);
		menu_view.add(mi_grid);
		menu_view.add(mi_middleLine);
		menu_view.setMnemonic('V');
		add(menu_view);

		menu_help.add(mi_about);
		menu_help.setMnemonic('H');
		add(menu_help);

	}//end createMenu

//=======================================================
	private void addActionListeners()
	{
		mi_open_file.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.processFile(m.showOpenFileDialog());
				m.mainframe.toFront();
			}
		});

		mi_save_image.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				///m.saveImage();
			}
		});

		mi_quit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});

		mi_mono.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(m.graph.isDisplayMono())
				{
					m.graph.setDisplayMono(false);
					mi_mono.setLabel("Mono (Overlayed)");
				}
				else
				{
					m.graph.setDisplayMono(true);
					mi_mono.setLabel("Multichannel");
				}
			}
		});

		mi_rectified.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(m.graph.isDisplayRectified())
				{
					m.graph.setDisplayRectified(false);
					mi_rectified.setLabel("Rectified");
				}
				else
				{
					m.graph.setDisplayRectified(true);
					mi_rectified.setLabel("Normal");
				}
			}
		});

		mi_grid.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(m.graph.isDisplayGrid())
				{
					m.graph.setDisplayGrid(false);
					mi_grid.setLabel("Show Grid");
				}
				else
				{
					m.graph.setDisplayGrid(true);
					mi_grid.setLabel("Hide Grid");
				}
			}
		});

		mi_middleLine.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(m.graph.isDisplayMiddleLine())
				{
					m.graph.setDisplayMiddleLine(false);
					mi_middleLine.setLabel("Show Middle Line");
				}
				else
				{
					m.graph.setDisplayMiddleLine(true);
					mi_middleLine.setLabel("Hide Middle Line");
				}
			}
		});

		mi_about.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.about.setVisible(true);
			}
		});
	}//end addActionListeners
}//end class AppMenu
