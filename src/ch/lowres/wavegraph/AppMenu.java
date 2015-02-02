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
	}//end addActionListeners
}//end class AppMenu
