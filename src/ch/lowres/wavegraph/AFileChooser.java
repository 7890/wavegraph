//tb/150119

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

//=======================================================
public class AFileChooser
{
	private static Main m;

	//filedialog "recently used" doesn't work in jre ~< 8
	public static FileDialog openFileDialog;//=new FileDialog(mainframe, "Select RIFF Wave File to Graph", FileDialog.LOAD);
	//native osx filedialog with shipped jre borked
	public static JFileChooser fileChooser;//=new JFileChooser();

	//public static String lastFileOpenDirectory=System.getProperty("user.dir");
	public static String lastFileOpenDirectory=m.os.getHomeDir();

//=======================================================
	public static String showOpenFileDialog(String baseDir, String file)
	{
		if(baseDir==null)
		{
			baseDir=lastFileOpenDirectory;
		}

		//filter shown files
		OpenFileFilter filter=new OpenFileFilter();
		filter.addExtension(".wav");
		filter.addExtension(".wavex");

		if(m.os.isMac())
		{
			if(fileChooser==null)
			{
				fileChooser=new JFileChooser();
				//fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
				fileChooser.setFileFilter(filter);
				fileChooser.setDialogTitle("Select RIFF Wave File to Graph");

				JPanel fcTools=new JPanel(new GridLayout(5,1));
				JButton buttonParentDirectory=new JButton("Up");
				JButton buttonHome=new JButton("Home");
				//JButton buttonDesktop=new JButton("Desktop");
				//JButton buttonMusic=new JButton("Music");
				JButton buttonRoot=new JButton("/");

				buttonParentDirectory.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						try{
							fileChooser.setCurrentDirectory(new File(fileChooser.getCurrentDirectory().getParent()));
						}catch(Exception ex){}
					}
				});

				buttonHome.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						fileChooser.setCurrentDirectory(new File(m.os.getHomeDir()));
					}
				});

				buttonRoot.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						fileChooser.setCurrentDirectory(new File("/"));
					}
				});

				fcTools.add(buttonParentDirectory);
				fcTools.add(new JLabel(" "));
				fcTools.add(buttonHome);
				//fcTools.add(buttonDesktop);
				//fcTools.add(buttonMusic);
				fcTools.add(buttonRoot);
				fcTools.add(new JLabel(" "));

				fileChooser.setAccessory(fcTools);

			}//end if not initialized

			if(baseDir!=null)
			{
				fileChooser.setCurrentDirectory(new File(baseDir));
			}
			if(file!=null && !file.equals(""))
			{
				fileChooser.setSelectedFile(new File(file));
			}

			while(true)
			{

			int retval=fileChooser.showDialog(m.mainframe,"Open");

			if(retval==JFileChooser.APPROVE_OPTION)
			{
				File f=fileChooser.getSelectedFile();

				if(f == null || !f.exists())
				{
					return null;
				}
				if(f.isDirectory())
				{
					//prepare for while loop
					fileChooser.setCurrentDirectory(f);
					fileChooser.setSelectedFile(null);
				}
				else
				{
					//remember for next open
					lastFileOpenDirectory=f.getParent();

					return f.getAbsolutePath();
				}
			}
			else
			{
				return null;
			}
		}//end while given "file" is directory ..

		}
		else//if not mac
		{
			if(openFileDialog==null)
			{
				openFileDialog=new FileDialog(m.mainframe, "Select RIFF Wave File to Graph", FileDialog.LOAD);
			}

			//filter shown files
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

	}//end showOpenFileDialog
}//end class AFileChooser
