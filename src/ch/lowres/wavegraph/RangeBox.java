//tb/150218

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import java.text.*;

//=======================================================
public class RangeBox extends JPanel
{
	//                         "X:  000 000 000"
	//                         "X: 00:00:00.000^"
	private String blankString="                ";
	private JLabel label_start=new JLabel(blankString,JLabel.CENTER);
	private JLabel label_end=new JLabel("",JLabel.CENTER);
	private JLabel label_length=new JLabel("",JLabel.CENTER);
	private JLabel label_caption=new JLabel("",JLabel.CENTER);
	private DecimalFormat df=new DecimalFormat("#,###,###,##0.00");

//=======================================================
	public RangeBox()
	{
		createGUI();
	}

//=======================================================
	public RangeBox(String caption)
	{
		label_caption.setText(caption);
		createGUI();
	}

//=======================================================
	private void createGUI()
	{
		setLayout(new GridLayout(4,1));
		//setPreferredSize(new Dimension(125,70));

		setOpaque(true);

		Border border=new BevelBorder(BevelBorder.RAISED);
		setBorder(border);

		label_caption.setOpaque(true);
		label_caption.setBackground(Colors.labelgroup_background);
		label_caption.setForeground(Colors.labelgroup_foreground);
		//label_caption.setFont(new JLabel().getFont().deriveFont(10f);

		//top, left, bottom, right
		label_start.setBorder(new EmptyBorder(1, 4, 1, 4));
		label_end.setBorder(new EmptyBorder(1, 4, 1, 4));
		label_length.setBorder(new EmptyBorder(1, 4, 1, 4));
		label_caption.setBorder(new EmptyBorder(1, 4, 1, 4));

		add(label_start);
		add(label_end);
		add(label_length);
		add(label_caption);
	}

//=======================================================
	public void resetFont(Font f)
	{
		label_start.setFont(f);
		label_end.setFont(f);
		label_length.setFont(f);
		label_caption.setFont(f.deriveFont(f.getSize2D()-3));
	}

//=======================================================
	public void setFormat(DecimalFormat format)
	{
		df=format;
	}

//=======================================================
	public void setCaption(String caption)
	{
		label_caption.setText(caption);
	}

//=======================================================
	public void blank()
	{
		label_start.setText(blankString);
		label_end.setText(blankString);
		label_length.setText(blankString);
	}

//=======================================================
	public void setValues(double start, double end, double length)
	{
		setStart(start);
		setEnd(end);
		setLength(length);
	}

//=======================================================
	public void setStart(double start)
	{
		label_start.setText("S:  "+padLeft(df.format(start),11)+" ");
	}

//=======================================================
	public void setEnd(double end)
	{
		label_end.setText("E:  "+padLeft(df.format(end),11)+" ");
	}

//=======================================================
	public void setLength(double length)
	{
		label_length.setText("L:  "+padLeft(df.format(length),11)+" ");
	}

//=======================================================
	public void setStart(String start)
	{
		label_start.setText(start);
	}

//=======================================================
	public void setEnd(String end)
	{
		label_end.setText(end);
	}

//=======================================================
	public void setLength(String length)
	{
		label_length.setText(length);
	}

//=======================================================
	//http://stackoverflow.com/questions/388461/how-can-i-pad-a-string-in-java
	public static String padRight(String s, int n)
	{
		return String.format("%1$-" + n + "s", s);
	}

//=======================================================
	public static String padLeft(String s, int n)
	{
		return String.format("%1$" + n + "s", s);
	}
}//end class RangeBox
