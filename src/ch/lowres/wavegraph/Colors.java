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

	public static Color wave_canvas_background=new Color(222,222,222);

/*
	//forms
	public static Color form_background=new Color(38,38,42);
	public static Color form_foreground=new Color(200,200,200);

	//textfields, lists
	public static Color input_background=new Color(230,230,233);
	public static Color input_foreground=new Color(1,2,3);

	//buttons
	public static Color button_background=new Color(240,140,100);
	public static Color button_foreground=new Color(22,12,2);

	//status bar
	public static Color status_background=new Color(20,220,20);
	public static Color status_foreground=new Color(0,0,0);

	public static Color status_error_background=red;
	public static Color status_error_foreground=white;

	//focused widget outline
	public static Color status_focused_outline=new Color(200,200,255);

	//hovered widget overlay
	public static Color hovered_overlay=new Color(150,160,240);

	//selected text (selection color)
	public static Color selected_text=new Color(200,200,255);

	//disabled text in fields
	public static Color disabled_text=gray;

	//ListDialog
	public static Color list_background=white;
	public static Color list_foreground=black;
	public static Color list_selected_background=red;
	public static Color list_selected_foreground=white;
	public static Color list_hovered_background=green;
	public static Color list_hovered_foreground=black;
*/
}//end class Colors
