//tb/1501

package ch.lowres.wavegraph;

//========================================================================
public class Mac
{
	private static Main g;

	//set dockname on osx:
	//java -Xdock:name="appname"

//========================================================================
	public static void init()
	{
		//AboutHandler, PreferencesHandler, AppReOpenedListener, OpenFilesHandler, PrintFilesHandler, QuitHandler, QuitResponse 
		com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();

		 //need to enable the preferences option manually
		application.setEnabledPreferencesMenu(true);

		application.setAboutHandler(new com.apple.eawt.AboutHandler()
		{
			@Override
			public void handleAbout(com.apple.eawt.AppEvent.AboutEvent e)
			{
				//javax.swing.JOptionPane.showMessageDialog(null, "hello");
				if(g.about!=null)
				{
					g.about.updateText();
					g.about.setVisible(true);
				}
			}
		});
/*
		application.setPreferencesHandler(new com.apple.eawt.PreferencesHandler()
		{
			@Override
			public void handlePreferences(com.apple.eawt.AppEvent.PreferencesEvent e)
			{
				if(g.configure!=null)
				{
					g.configure.setVisible(true);
				}
			}
		});
*/

		//only works with Aqua Look and Feel
		//application.setDefaultMenuBar(g.applicationMenu);
	}//end init
}//end class Mac