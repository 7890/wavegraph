//tb/14

package ch.lowres.wavegraph;

import java.awt.*;
import javax.swing.*;
import java.awt.geom.AffineTransform;

import java.io.*;

/**
* Holding colors to be accessed statically by any component.
*/
//========================================================================
public class Fonts
{
	static Main m;

	public static Font fontNormal;
	public static Font fontLarge;

	public static boolean use_internal_font=true;

	public static String fontName="Dialog";

	//~points
	public static float fontDefaultSize=11f;
	//relative to fontDefaultSize
	public static float fontLargeFactor=1.0f;

	//in native java @72dpi point units
	public static float fontNormalSize=dpiCorrectedPt(fontDefaultSize);
	public static float fontLargeSize=fontNormalSize*fontLargeFactor;

	public static int fontNormalStyle=Font.PLAIN;
	public static int fontLargeStyle=Font.BOLD;

	public static String[] styles = {"Regular", "Bold", "Italic", "Bold Italic"};

//========================================================================
	public static void set(String font)
	{
///////////
		//check if exists on system
		fontName=font;
	}

//========================================================================
	public static String[] getAll()
	{
		GraphicsEnvironment gEnv=GraphicsEnvironment.getLocalGraphicsEnvironment();
		return gEnv.getAvailableFontFamilyNames();
	}

//========================================================================
	public static void init()//float fontsize, String fontname)
	{
		//in native java @72dpi point units
		fontNormalSize=dpiCorrectedPt(fontDefaultSize);
		fontLargeSize=fontNormalSize*fontLargeFactor;

		if(use_internal_font)
		{
			fontNormal=getDefaultFont(fontNormalSize,fontNormalStyle);
			fontLarge=getDefaultFont(fontLargeSize,fontLargeStyle);
		}
		else
		{
			fontNormal=new Font(fontName,fontNormalStyle,(int)fontNormalSize);
			fontLarge=new Font(fontName,fontLargeStyle,(int)fontLargeSize);
		}
	}

//========================================================================
	//http://www.java2s.com/Tutorials/Java/java.awt/GraphicsConfiguration/Java_GraphicsConfiguration_getNormalizingTransform_.htm
	//https://www.linkedin.com/pulse/20140715044516-153509697-java-interview-questions
	public static float dpiCorrectedPt(float at72)
	{
		//return (at72 * m.os.getDPI() / 72f);

		GraphicsEnvironment gEnv=GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gs = gEnv.getDefaultScreenDevice();
		GraphicsConfiguration gc = gs.getDefaultConfiguration();

		AffineTransform at=gc.getNormalizingTransform();
		return (float)(Math.max(at.getScaleX(),at.getScaleY()) * at72);
	}

//========================================================================
	public static void setDefaultSize()
	{
		fontDefaultSize=11f;
		fontNormalSize=dpiCorrectedPt(fontDefaultSize);
		fontNormal=fontNormal.deriveFont(fontNormalSize);
		change(m.mainframe);
		change(m.about);
		m.about.updateText();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m.graph.forceRepaint();
			}
		});
	}

//========================================================================
	public static void increaseSize()
	{
		fontNormalSize+=2f;
		fontNormal=fontNormal.deriveFont(fontNormalSize);
		change(m.mainframe);
		change(m.about);
		m.about.updateText();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m.graph.forceRepaint();
			}
		});
	}

//========================================================================
	public static void decreaseSize()
	{
		fontNormalSize-=2f;
		fontNormalSize=Math.max(6,fontNormalSize);
		fontNormal=fontNormal.deriveFont(fontNormalSize);
		change(m.mainframe);
		change(m.about);
		m.about.updateText();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m.graph.forceRepaint();
			}
		});
	}

//========================================================================
	//http://stackoverflow.com/questions/12730230/set-the-same-font-for-all-component-java
	//http://stackoverflow.com/questions/22494495/change-font-dynamically-causes-problems-on-some-components
	public static void change(Component component)
	{
		if (component instanceof JMenu)
		{
			//change menu font
			component.setFont(fontNormal);

			//menuitems need special treatment, getMenuComponents
			Component[] children=null;
			children=((JMenu)component).getMenuComponents();

			for(Component child : children)
			{
				//recurse
				change(child);
			}
			return;
		}
/*
		else if (component instanceof JLabel)
		{
			return;
		}
*/
		else if(component instanceof JButton)
		{
			component.setFont(fontLarge);
			return;
		}
		else if(component instanceof RangeBox)
		{
			((RangeBox)component).resetFont(fontNormal);
			return;
		}
		else
		{
			component.setFont(fontNormal);
		}

		if(component instanceof Container)
		{
			for (Component child : ((Container)component).getComponents())
			{
				change(child);
			}
		}
	}//end change

//========================================================================
	static String defaultFontUri="/resources/fonts/UbuntuMono-R.ttf";
	public static Font getDefaultFont(float fontSize)
	{
		return createFontFromJar(defaultFontUri,fontSize);
	}

//========================================================================
	public static Font getDefaultFont(float fontSize, int style) //Font.BOLD
	{
		return createFontFromJar(defaultFontUri,fontSize,style);
	}

//========================================================================
	public static Font createFontFromJar(String fontUriInJar, float fontSize)
	{
		return createFontFromJar(fontUriInJar, fontSize, Font.PLAIN);
	}

//========================================================================
	public static Font createFontFromJar(String fontUriInJar, float fontSize, int style)
	{
		InputStream is;
		Font f;
		try
		{
			is=Fonts.class.getResourceAsStream(fontUriInJar);
			f=Font.createFont(Font.TRUETYPE_FONT, is);
			f=f.deriveFont(style,fontSize);
			is.close();
		}
		catch(Exception e)
		{
			m.w("could not load built-in font. "+e.getMessage());
			return null;
		}
		return f;
	}//end createFontFromJar
}//end class Fonts
