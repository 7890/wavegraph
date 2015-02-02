//tb/14

package ch.lowres.wavegraph;

import java.awt.Toolkit;

/**
* Read basic platform specific properties, processor architecture.
*/
//========================================================================
public class OSTest
{
	private static boolean isUnix;
	private static boolean isWindows;
	private static boolean isMac;
	private static boolean is32Bits;
	private static boolean is64Bits;

//========================================================================
	private static void determineOS()
	{
		String os = System.getProperty("os.name").toLowerCase();
		isUnix = os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0;
		isWindows = os.indexOf("win") >= 0;
		isMac = os.indexOf("mac") >= 0;
	}

//========================================================================
	//https://community.oracle.com/thread/2086185?start=0&tstart=0
	private static void determineArch()
	{
		String bits = System.getProperty("sun.arch.data.model", "?");
		if(bits.equals("64"))
		{
			is64Bits=true;
			is32Bits=false;
			return;
		}
		else if(bits.equals("?"))
		{
			// probably sun.arch.data.model isn't available
			// maybe not a Sun JVM?
			// try with the vm.name property
			is64Bits=System.getProperty("java.vm.name").toLowerCase().indexOf("64") >= 0;
			is32Bits=!is64Bits;
			return;
		}
		// probably 32bit
		is32Bits=true;
		is32Bits=false;
	}

//========================================================================
	public static String getOSName()
	{
		return System.getProperty("os.name");
	}

//========================================================================
	public static String getVMName()
	{
		return System.getProperty("java.vm.name");
	}

//========================================================================
	public static String getJavaVersion()
	{
		return System.getProperty("java.version");
	}

//========================================================================
	public static String getVMVersion()
	{
		return System.getProperty("java.vm.version");
	}

//========================================================================
	public static String getTempDir()
	{
		return System.getProperty("java.io.tmpdir");
	}

//========================================================================
	public static String getHomeDir()
	{
		return System.getProperty("user.home");
	}

//========================================================================
	public static int getDPI()
	{
		return Toolkit.getDefaultToolkit().getScreenResolution();
	}

//========================================================================
	public static boolean isUnix()
	{
		determineOS();
		return isUnix;
	}

//========================================================================
	public static boolean isLinux()
	{
		determineOS();
		return isUnix;
	}

//========================================================================
	public static boolean isMac()
	{
		determineOS();
		return isMac;
	}

//========================================================================
	public static boolean isWindows()
	{
		determineOS();
		return isWindows;
	}

//========================================================================
	public static boolean is32Bits()
	{
		determineArch();
		return is32Bits;
	}

//========================================================================
	public static boolean is64Bits()
	{
		determineArch();
		return is64Bits;
	}
} //end class OSTest
