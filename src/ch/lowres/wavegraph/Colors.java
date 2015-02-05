//tb/14x

package ch.lowres.wavegraph;

import java.awt.Color;

/**
* Holding colors to be accessed statically by any component.
*/
//========================================================================
public class Colors
{
	public static Color red=new Color(255,0,0);
	public static Color green=new Color(0,255,0);
	public static Color blue=new Color(0,0,255);
	public static Color white=new Color(255,255,255);
	public static Color black=new Color(0,0,0);
	public static Color gray=new Color(120,120,120);

	public static Color wave_background=new Color(250,250,250);
	public static Color wave_foreground=new Color(0,0,0);

	public static Color wave_delimiter_top=gray.darker();
	public static Color wave_delimiter_bottom=gray.darker().darker();

	public static Color wave_zeroline=gray;

	public static Color wave_grid=new Color(215,215,255);
	public static Color canvas_grid=wave_grid.darker();

	public static Color wave_canvas_background=new Color(222,222,222);

	public static Color labelgroup_background=Colors.black.brighter();
	public static Color labelgroup_foreground=Colors.white.darker();

	public static Color infopanel_background=new Color(215,215,215);

/*
	//ListDialog
	public static Color list_background=white;
	public static Color list_foreground=black;
	public static Color list_selected_background=red;
	public static Color list_selected_foreground=white;
	public static Color list_hovered_background=green;
	public static Color list_hovered_foreground=black;
*/
}//end class Colors
