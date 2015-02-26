//tb/150119

import ch.lowres.wavegraph.*;

//=======================================================
public class Wavegraph
{
	private static Main m;

//=======================================================
	public static void main(String[] args)
	{
		p("~~~");
		p("welcome to "+m.progName+"!");
		p("by Thomas Brand <tom@trellis.ch>");
		p(m.progHome);

		p("");
		p("command line argument (optional): (1) file to load");
		p("if file is directory, a file chooser dialog will be shown.");

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
	public static void p(String s)
	{
		System.out.println(s);
	}

//=======================================================
	public static void w(String s)
	{
		System.out.println("/!\\ "+s);
	}
}//end class Wavegraph
