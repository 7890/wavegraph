//tb/1501

package ch.lowres.wavegraph;

import java.awt.Window;
import java.lang.reflect.*;

//========================================================================
public class Mac
{
	private static Main m;

	//set dockname on osx:
	//java -Xdock:name="appname"

//========================================================================
	public static void init()
	{
		//set name in osx menu (only works if called before any widget created)
		//System.setProperty("com.apple.mrj.application.apple.menu.about.name",m.progName);

		//AboutHandler, PreferencesHandler, AppReOpenedListener, OpenFilesHandler, PrintFilesHandler, QuitHandler, QuitResponse 
		com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();

		 //need to enable the preferences option manually
		//application.setEnabledPreferencesMenu(true);

		application.setAboutHandler(new com.apple.eawt.AboutHandler()
		{
			@Override
			public void handleAbout(com.apple.eawt.AppEvent.AboutEvent e)
			{
				//javax.swing.JOptionPane.showMessageDialog(null, "hello");
				if(m.about!=null)
				{
					m.about.setVisible(true);
				}
			}
		});
/*
		application.setPreferencesHandler(new com.apple.eawt.PreferencesHandler()
		{
			@Override
			public void handlePreferences(com.apple.eawt.AppEvent.PreferencesEvent e)
			{
				if(m.configure!=null)
				{
					m.configure.setVisible(true);
				}
			}
		});
*/

		//only works with Aqua Look and Feel
		//application.setDefaultMenuBar(m.applicationMenu);

		enableFullScreenMode(m.mainframe);

	}//end init

//========================================================================
	//http://saipullabhotla.blogspot.ch/2012/05/enabling-full-screen-mode-for-java.html
	public static void enableFullScreenMode(Window window)
	{
		String className = "com.apple.eawt.FullScreenUtilities";
		String methodName = "setWindowCanFullScreen";
		try
		{
			Class<?> clazz = Class.forName(className);
			Method method = clazz.getMethod(methodName, new Class<?>[]
				{Window.class, boolean.class });
			method.invoke(null, window, true);
		}
		catch(Throwable t)
		{
			System.err.println("Full screen mode is not supported");
			t.printStackTrace();
		}
	}
}//end class Mac
