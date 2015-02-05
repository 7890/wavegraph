//tb/150123

package ch.lowres.wavegraph;

import java.io.*;
import java.util.*;

//hybrid
public class OpenFileFilter 
	extends javax.swing.filechooser.FileFilter
	implements java.io.FilenameFilter
{
	private ArrayList<String> extensions = new ArrayList<String>();

	//not all platforms support all features in native file dialog
	//-set filter
	//-set initially selected file
	//-use "recent" files

//=======================================================
	public OpenFileFilter()
	{
	}

//=======================================================
	public OpenFileFilter(String extension)
	{
		extensions.add(extension);
	}

//=======================================================
	public void addExtension(String extension)
	{
		//extention including . -> i.e. ".wav"(implicit end)
		extensions.add(extension);
	}

//=======================================================
	public boolean accept(File dir, String name)
	{
		if(extensions.isEmpty())
		{
			return true;
		}

		//case insensitive
		for(String ext : extensions)
		{
			if(name.toLowerCase().endsWith(ext))
			{
				return true;
			}
		}
		return false;
	}

//=======================================================
	public boolean accept(File file)
	{
		if(extensions.isEmpty())
		{
			return true;
		}
		if(file.isDirectory())
		{
			return true;
		}
		//case insensitive
		for(String ext : extensions)
		{
			if(file.getName().toLowerCase().endsWith(ext))
			{
				return true;
			}
		}
		return false;
	}

//=======================================================
	public String getDescription()
	{
		return "Wave Files (.wav)";
	}

}//end class OpenFileFilter
