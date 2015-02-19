//tb/150218

package ch.lowres.wavegraph;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.text.*;

//=======================================================
public class RangeBox extends JPanel
{
	private JLabel label_start=new JLabel("",JLabel.CENTER);
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
		setPreferredSize(new Dimension(125,70));
		setOpaque(true);

		label_caption.setOpaque(true);
		label_caption.setBackground(Colors.labelgroup_background);
		label_caption.setForeground(Colors.labelgroup_foreground);
		label_caption.setFont(new JLabel().getFont().deriveFont(10f));

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
		label_caption.setFont(f.deriveFont(10f));
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
	public void setValues(double start, double end, double length)
	{
		setStart(start);
		setEnd(end);
		setLength(length);
	}

//=======================================================
	public void setStart(double start)
	{
		label_start.setText("S: "+df.format(start));
	}

//=======================================================
	public void setEnd(double end)
	{
		label_end.setText("E: "+df.format(end));
	}

//=======================================================
	public void setLength(double length)
	{
		label_length.setText("L: "+df.format(length));
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
}//end class RangeBox
