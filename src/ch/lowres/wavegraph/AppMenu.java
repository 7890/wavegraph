//tb/150122

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.io.*;
import java.util.*;

public class AppMenu extends JMenuBar
{
	private static JMenu menu_file=new JMenu("File");

	private static JMenuItem mi_open_file=new JMenuItem("Open...");

	private static JMenu sub_file_recent=new JMenu("Recent");
	private static int maxRecentEntries=15;
	private static JMenuItem mi_recent[]=new JMenuItem[maxRecentEntries];
	private static ArrayList<File> recentFiles = new ArrayList<File>();

//	private static JMenuItem mi_save_image=new JMenuItem("Save As Image...");
	private static JMenuItem mi_quit=new JMenuItem("Quit");

//	private static JMenu menu_edit=new JMenu("Edit");

	private static JMenu sub_edit_selection=new JMenu("Selection");

	private static JMenu sub_edit_selection_trim=new JMenu("Trim");
	private static JMenu sub_edit_selection_trim_start=new JMenu("Start");
	private static JMenu sub_edit_selection_trim_end=new JMenu("End");

	private static JMenu sub_edit_selection_align=new JMenu("Align");
	private static JMenu sub_edit_selection_align_start=new JMenu("Start");
	private static JMenu sub_edit_selection_align_end=new JMenu("End");

	private static JMenu sub_edit_selection_nudge=new JMenu("Nudge");
	private static JMenuItem mi_nudge_selection_forward=new JMenuItem("Forward");
	private static JMenuItem mi_nudge_selection_backward=new JMenuItem("Backward");

	private static JMenu sub_edit_selection_create=new JMenu("Create");
	private static JMenuItem mi_create_selection_viewport=new JMenuItem("All In Viewport");

	private static JMenuItem mi_clear_selection=new JMenuItem("Clear");

	private static JMenuItem mi_trim_selection_start_to_edit_point=new JMenuItem("To Edit Point");
	private static JMenuItem mi_trim_selection_start_double=new JMenuItem("Double");
	private static JMenuItem mi_trim_selection_start_halve=new JMenuItem("Halve");

	private static JMenuItem mi_trim_selection_end_to_edit_point=new JMenuItem("To Edit Point");
	private static JMenuItem mi_trim_selection_end_double=new JMenuItem("Double");
	private static JMenuItem mi_trim_selection_end_halve=new JMenuItem("Halve");

	private static JMenuItem mi_align_selection_start_to_edit_point=new JMenuItem("To Edit Point");
	private static JMenuItem mi_align_selection_end_to_edit_point=new JMenuItem("To Edit Point");

	private static JMenu menu_view=new JMenu("View");

	private static JMenu sub_view_window=new JMenu("Window");

	private static JMenu sub_view_window_info_top=new JMenu("Infopanel Top");
	private static JMenuItem mi_show_info_top=new JMenuItem("Show");
	private static JMenuItem mi_hide_info_top=new JMenuItem("Hide");

	private static JMenu sub_view_window_info_bottom=new JMenu("Infopanel Bottom");
	private static JMenuItem mi_show_info_bottom=new JMenuItem("Show");
	private static JMenuItem mi_hide_info_bottom=new JMenuItem("Hide");

	private static JMenu sub_view_window_menu=new JMenu("Menu");
	private static JMenuItem mi_hide_menu=new JMenuItem("Hide ('ESC' To Revert)");

	private static JMenu sub_view_window_decoration=new JMenu("Decoration");
	private static JMenuItem mi_show_decoration=new JMenuItem("Show");
	private static JMenuItem mi_hide_decoration=new JMenuItem("Hide");

	private static JMenu sub_view_canvas=new JMenu("Wave Canvas");

	private static JMenu sub_view_presets=new JMenu("Presets");
	private static JMenuItem mi_preset_default=new JMenuItem("Default");
	private static JMenuItem mi_preset_mono_rectified_filled=new JMenuItem("Mono Rectified Filled");

	private static JMenu sub_view_channels=new JMenu("Channels");
	private static JMenuItem mi_multichannel=new JMenuItem("Multichannel");
	private static JMenuItem mi_mono=new JMenuItem("Mono (Overlayed)");

	private static JMenu sub_view_alignment=new JMenu("Alignment");
	private static JMenuItem mi_traditional=new JMenuItem("Traditional");
	private static JMenuItem mi_rectified=new JMenuItem("Rectified");

	private static JMenu sub_view_style=new JMenu("Style");
	private static JMenuItem mi_lines=new JMenuItem("Lines");
	private static JMenuItem mi_filled=new JMenuItem("Filled");

	private static JMenu sub_view_grid=new JMenu("Grid");
	private static JMenuItem mi_show_grid=new JMenuItem("Show");
	private static JMenuItem mi_hide_grid=new JMenuItem("Hide");

	private static JMenu sub_view_middleLine=new JMenu("Middle Line");
	private static JMenuItem mi_show_middleLine=new JMenuItem("Show");
	private static JMenuItem mi_hide_middleLine=new JMenuItem("Hide");

	private static JMenu sub_view_limits=new JMenu("Limits");
	private static JMenuItem mi_show_limits=new JMenuItem("Show");
	private static JMenuItem mi_hide_limits=new JMenuItem("Hide");

	private static JMenu sub_view_gap=new JMenu("Separation Gap");
	private static JMenuItem mi_show_gap=new JMenuItem("On");
	private static JMenuItem mi_hide_gap=new JMenuItem("Off");

	private static JMenu sub_view_scrollbar=new JMenu("Scrollbar");
	private static JMenuItem mi_show_scrollbar=new JMenuItem("Show");
	private static JMenuItem mi_hide_scrollbar=new JMenuItem("Hide");

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

/*
		mi_save_image.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_S, m.ctrlOrCmd));
*/

		mi_quit.setAccelerator(
			KeyStroke.getKeyStroke(KeyEvent.VK_Q, m.ctrlOrCmd));

		menu_file.add(mi_open_file);

		for(int i=0;i<maxRecentEntries;i++)
		{
			mi_recent[i]=new JMenuItem("(empty)");
			mi_recent[i].setVisible(false);

			sub_file_recent.add(mi_recent[i]);
			sub_file_recent.setEnabled(false);
		}

		menu_file.add(sub_file_recent);
//		menu_file.add(mi_save_image);
		menu_file.add(new JSeparator());
		menu_file.add(mi_quit);
		menu_file.setMnemonic('F');
		add(menu_file);

		sub_edit_selection_trim_start.add(mi_trim_selection_start_to_edit_point);
		sub_edit_selection_trim_start.add(mi_trim_selection_start_double);
		sub_edit_selection_trim_start.add(mi_trim_selection_start_halve);
		sub_edit_selection_trim.add(sub_edit_selection_trim_start);

		sub_edit_selection_trim_end.add(mi_trim_selection_end_to_edit_point);
		sub_edit_selection_trim_end.add(mi_trim_selection_end_double);
		sub_edit_selection_trim_end.add(mi_trim_selection_end_halve);
		sub_edit_selection_trim.add(sub_edit_selection_trim_end);

		sub_edit_selection_align_start.add(mi_align_selection_start_to_edit_point);
		sub_edit_selection_align.add(sub_edit_selection_align_start);

		sub_edit_selection_align_end.add(mi_align_selection_end_to_edit_point);
		sub_edit_selection_align.add(sub_edit_selection_align_end);

		sub_edit_selection_nudge.add(mi_nudge_selection_forward);
		sub_edit_selection_nudge.add(mi_nudge_selection_backward);

		sub_edit_selection.add(sub_edit_selection_trim);
		sub_edit_selection.add(sub_edit_selection_align);
		sub_edit_selection.add(sub_edit_selection_nudge);

		sub_edit_selection_create.add(mi_create_selection_viewport);
		sub_edit_selection.add(sub_edit_selection_create);

		sub_edit_selection.add(mi_clear_selection);

//		menu_edit.add(sub_edit_selection);
//		menu_edit.setMnemonic('E');
//		add(menu_edit);

		sub_edit_selection.setMnemonic('S');
		add(sub_edit_selection);

		sub_view_window_info_top.add(mi_show_info_top);
		sub_view_window_info_top.add(mi_hide_info_top);
		mi_show_info_top.setEnabled(false);
		sub_view_window.add(sub_view_window_info_top);

		sub_view_window_info_bottom.add(mi_show_info_bottom);
		sub_view_window_info_bottom.add(mi_hide_info_bottom);
		mi_show_info_bottom.setEnabled(false);
		sub_view_window.add(sub_view_window_info_bottom);

		sub_view_window_menu.add(mi_hide_menu);
		sub_view_window.add(sub_view_window_menu);

		sub_view_window_decoration.add(mi_show_decoration);
		sub_view_window_decoration.add(mi_hide_decoration);
		mi_show_decoration.setEnabled(false);
		sub_view_window.add(sub_view_window_decoration);

		menu_view.add(sub_view_window);

		sub_view_presets.add(mi_preset_default);
		sub_view_presets.add(mi_preset_mono_rectified_filled);
		sub_view_canvas.add(sub_view_presets);

		sub_view_channels.add(mi_multichannel);
		sub_view_channels.add(mi_mono);
		mi_multichannel.setEnabled(false);
		sub_view_canvas.add(sub_view_channels);

		sub_view_alignment.add(mi_traditional);
		sub_view_alignment.add(mi_rectified);
		sub_view_canvas.add(sub_view_alignment);
		mi_traditional.setEnabled(false);

		sub_view_style.add(mi_lines);
		sub_view_style.add(mi_filled);
		sub_view_canvas.add(sub_view_style);
		mi_lines.setEnabled(false);

		sub_view_grid.add(mi_show_grid);
		sub_view_grid.add(mi_hide_grid);
		sub_view_canvas.add(sub_view_grid);
		mi_show_grid.setEnabled(false);

		sub_view_middleLine.add(mi_show_middleLine);
		sub_view_middleLine.add(mi_hide_middleLine);
		sub_view_canvas.add(sub_view_middleLine);
		mi_show_middleLine.setEnabled(false);

		sub_view_limits.add(mi_show_limits);
		sub_view_limits.add(mi_hide_limits);
		sub_view_canvas.add(sub_view_limits);
		mi_show_limits.setEnabled(false);

		sub_view_gap.add(mi_show_gap);
		sub_view_gap.add(mi_hide_gap);
		sub_view_canvas.add(sub_view_gap);
		mi_show_gap.setEnabled(false);

		sub_view_scrollbar.add(mi_show_scrollbar);
		sub_view_scrollbar.add(mi_hide_scrollbar);
		sub_view_canvas.add(sub_view_scrollbar);
		mi_show_scrollbar.setEnabled(false);

		menu_view.add(sub_view_canvas);

		menu_view.setMnemonic('V');
		add(menu_view);

		menu_help.add(mi_about);
		menu_help.setMnemonic('H');
		add(menu_help);
	}//end createMenu

//=======================================================
	public JMenuItem getFileMenu()
	{
		return menu_file;
	}

//=======================================================
	public JMenuItem getSelectionMenu()
	{
		return sub_edit_selection;
	}

//=======================================================
	public JMenuItem getViewMenu()
	{
		return menu_view;
	}

//=======================================================
	public void addRecentFile(File f)
	{
		if(f.exists())
		{
			//check first if already in list, if yes, move to top
			if(recentFiles.contains(f))
			{
				//m.p("file arelady in list!");
				recentFiles.remove(f);
				recentFiles.add(0,f);
			}
			else
			{
				recentFiles.add(0,f);
			}

			//check if old recent files need to be removed from list
			if(recentFiles.size()>=maxRecentEntries)
			{
				//m.p("need to remove from recent file list!");
				while(recentFiles.size()>maxRecentEntries)
				{
					recentFiles.remove(recentFiles.size()-1);
				}
			}
		}
		else
		{
			//if not existing (anymore) and was in list, remove
			recentFiles.remove(f);
		}
		consolidateRecentMenu();
	}

//=======================================================
	private void consolidateRecentMenu()
	{
		for(int i=0;i<maxRecentEntries;i++)
		{
			mi_recent[i].setVisible(false);
		}

		for(int i=0;i<Math.min(maxRecentEntries,recentFiles.size());i++)
		{
			mi_recent[i].setLabel(recentFiles.get(i).getAbsolutePath());
			mi_recent[i].setVisible(true);
		}

		if(recentFiles.size()>0)
		{
			sub_file_recent.setEnabled(true);
		}
		else
		{
			sub_file_recent.setEnabled(false);
		}
	}

//=======================================================
	private void openRecentFileAt(int index)
	{
		if(index>=0 && index<recentFiles.size())
		{
			m.processFile(recentFiles.get(index).getAbsolutePath());
		}
	}

//=======================================================
	HashMap<JMenuItem, Integer> itemMap = new HashMap<JMenuItem, Integer>();

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
/*
		mi_save_image.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				///m.saveImage();
			}
		});
*/
		//http://stackoverflow.com/questions/2430008/having-a-different-action-for-each-button-dynamically-created-in-a-loop
		//(can't use counter i as it's non-final)
		for(int i=0;i<maxRecentEntries;i++)
		{
			itemMap.put(mi_recent[i], new Integer(i));

			mi_recent[i].addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					Integer index = itemMap.get(e.getSource());
					openRecentFileAt(index);
				}
			});
		}

		mi_quit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});

		mi_trim_selection_start_to_edit_point.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.trimSelectionRangeStart();
			}
		});

		mi_trim_selection_start_double.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.doubleSelectionRangeLeft();
			}
		});

		mi_trim_selection_start_halve.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.halveSelectionRangeLeft();
			}
		});

		mi_trim_selection_end_to_edit_point.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.trimSelectionRangeEnd();
			}
		});

		mi_trim_selection_end_double.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.doubleSelectionRangeRight();
			}
		});

		mi_trim_selection_end_halve.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.halveSelectionRangeRight();
			}
		});

		mi_align_selection_start_to_edit_point.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.alignSelectionRangeStart();
			}
		});

		mi_align_selection_end_to_edit_point.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.alignSelectionRangeEnd();
			}
		});

		mi_nudge_selection_forward.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.nudgeSelectionRangeRight();
			}
		});

		mi_nudge_selection_backward.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.nudgeSelectionRangeLeft();
			}
		});


		mi_create_selection_viewport.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.selectAllInViewport();
			}
		});

		mi_clear_selection.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.clearSelectionRange();
			}
		});

		mi_show_decoration.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.setDecorated(true);
				mi_show_decoration.setEnabled(false);
				mi_hide_decoration.setEnabled(true);
			}
		});

		mi_hide_decoration.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.setDecorated(false);
				mi_hide_decoration.setEnabled(false);
				mi_show_decoration.setEnabled(true);
			}
		});

		mi_hide_menu.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.showMenu(false);
			}
		});

		mi_show_info_top.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.showInfoTop(true);
				mi_show_info_top.setEnabled(false);
				mi_hide_info_top.setEnabled(true);
			}
		});

		mi_hide_info_top.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.showInfoTop(false);
				mi_hide_info_top.setEnabled(false);
				mi_show_info_top.setEnabled(true);
			}
		});

		mi_show_info_bottom.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.showInfoBottom(true);
				mi_show_info_bottom.setEnabled(false);
				mi_hide_info_bottom.setEnabled(true);
			}
		});

		mi_hide_info_bottom.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.showInfoBottom(false);
				mi_hide_info_bottom.setEnabled(false);
				mi_show_info_bottom.setEnabled(true);
			}
		});

		mi_preset_default.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayMono(false);
				m.graph.setDisplayRectified(false);
				m.graph.setDisplayFilled(false);
				m.graph.setDisplayGrid(true);
				m.graph.setDisplayMiddleLine(true);
				m.graph.setDisplayLimits(true);
				m.graph.setDisplayGap(true);
				m.graph.setDisplayScrollbar(true);

				mi_multichannel.setEnabled(false);
				mi_mono.setEnabled(true);
				mi_traditional.setEnabled(false);
				mi_rectified.setEnabled(true);
				mi_lines.setEnabled(false);
				mi_filled.setEnabled(true);
				mi_show_grid.setEnabled(false);
				mi_hide_grid.setEnabled(true);
				mi_show_middleLine.setEnabled(false);
				mi_hide_middleLine.setEnabled(true);
				mi_show_limits.setEnabled(false);
				mi_hide_limits.setEnabled(true);
				mi_show_gap.setEnabled(false);
				mi_hide_gap.setEnabled(true);
				mi_show_scrollbar.setEnabled(false);
				mi_hide_scrollbar.setEnabled(true);
			}
		});

		mi_preset_mono_rectified_filled.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayMono(true);
				m.graph.setDisplayRectified(true);
				m.graph.setDisplayFilled(true);
				m.graph.setDisplayGrid(true);
				m.graph.setDisplayMiddleLine(false);
				m.graph.setDisplayLimits(true);
				m.graph.setDisplayGap(true);
				m.graph.setDisplayScrollbar(true);

				mi_multichannel.setEnabled(true);
				mi_mono.setEnabled(false);
				mi_traditional.setEnabled(true);
				mi_rectified.setEnabled(false);
				mi_lines.setEnabled(true);
				mi_filled.setEnabled(false);
				mi_show_grid.setEnabled(false);
				mi_hide_grid.setEnabled(true);
				mi_show_middleLine.setEnabled(true);
				mi_hide_middleLine.setEnabled(false);
				mi_show_limits.setEnabled(false);
				mi_hide_limits.setEnabled(true);
				mi_show_gap.setEnabled(false);
				mi_hide_gap.setEnabled(true);
				mi_show_scrollbar.setEnabled(false);
				mi_hide_scrollbar.setEnabled(true);
			}
		});

		mi_multichannel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayMono(false);
				mi_multichannel.setEnabled(false);
				mi_mono.setEnabled(true);
			}
		});

		mi_mono.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayMono(true);
				mi_mono.setEnabled(false);
				mi_multichannel.setEnabled(true);

			}
		});

		mi_traditional.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayRectified(false);
				mi_traditional.setEnabled(false);
				mi_rectified.setEnabled(true);
			}
		});

		mi_rectified.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayRectified(true);
				mi_rectified.setEnabled(false);
				mi_traditional.setEnabled(true);
			}
		});

		mi_lines.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayFilled(false);
				mi_lines.setEnabled(false);
				mi_filled.setEnabled(true);
			}
		});

		mi_filled.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayFilled(true);
				mi_filled.setEnabled(false);
				mi_lines.setEnabled(true);
			}

		});

		mi_show_grid.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayGrid(true);
				mi_show_grid.setEnabled(false);
				mi_hide_grid.setEnabled(true);
			}
		});

		mi_hide_grid.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayGrid(false);
				mi_hide_grid.setEnabled(false);
				mi_show_grid.setEnabled(true);
			}
		});

		mi_show_middleLine.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayMiddleLine(true);
				mi_show_middleLine.setEnabled(false);
				mi_hide_middleLine.setEnabled(true);
			}
		});

		mi_hide_middleLine.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayMiddleLine(false);
				mi_hide_middleLine.setEnabled(false);
				mi_show_middleLine.setEnabled(true);
			}
		});

		mi_show_limits.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayLimits(true);
				mi_show_limits.setEnabled(false);
				mi_hide_limits.setEnabled(true);
			}
		});

		mi_hide_limits.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayLimits(false);
				mi_hide_limits.setEnabled(false);
				mi_show_limits.setEnabled(true);
			}
		});

		mi_show_gap.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayGap(true);
				mi_show_gap.setEnabled(false);
				mi_hide_gap.setEnabled(true);
			}
		});

		mi_hide_gap.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayGap(false);
				mi_hide_gap.setEnabled(false);
				mi_show_gap.setEnabled(true);
			}
		});

		mi_show_scrollbar.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayScrollbar(true);
				mi_show_scrollbar.setEnabled(false);
				mi_hide_scrollbar.setEnabled(true);
			}
		});

		mi_hide_scrollbar.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.graph.setDisplayScrollbar(false);
				mi_hide_scrollbar.setEnabled(false);
				mi_show_scrollbar.setEnabled(true);
			}
		});

		mi_about.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m.about.setVisible(true);
			}
		});

		//"turn-off" mouse sensitivity of wave canvas while menu operated
		//if clicked anywhere on canvas while menu is open, click will get ignored, menu cancelled
		MenuListener menuListener=new MenuListener()
		{
			@Override
			public void menuSelected(MenuEvent arg0)
			{
				m.graph.removeMouseListeners();
			}
			@Override
			public void menuDeselected(MenuEvent arg0)
			{
				m.graph.addMouseListeners();
			}
			@Override
			public void menuCanceled(MenuEvent arg0)
			{
				m.graph.addMouseListeners();
			}
		};

		//add to main menus
		menu_file.addMenuListener(menuListener);
		sub_edit_selection.addMenuListener(menuListener);
		menu_view.addMenuListener(menuListener);
		menu_help.addMenuListener(menuListener);
	}//end addActionListeners

//=======================================================
	public void setNoFileLoaded()
	{
		sub_edit_selection.setEnabled(false);
		sub_view_canvas.setEnabled(false);
	}

//=======================================================
	public void setFileLoaded()
	{
		sub_edit_selection.setEnabled(true);
		sub_view_canvas.setEnabled(true);		
	}
}//end class AppMenu
