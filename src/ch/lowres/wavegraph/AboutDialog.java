//tb/1501

package ch.lowres.wavegraph;

import java.awt.*;

//========================================================================
public class AboutDialog extends ADialog
{
//========================================================================
	public AboutDialog(Frame f, String title, boolean modality) 
	{
		super(f,title,modality);
		getTextPane().setHighlighter(null);
	}

//========================================================================
	public String getHtml()
	{
		StringBuffer sb=new StringBuffer();
		sb.append("<html><body>");

		sb.append("<p><strong>"+"About  -- ABSOLUTE TEST RELEASE"+"</strong><br>");
		sb.append("<br>");
		//http://stackoverflow.com/questions/9117814/jtextpane-with-html-local-image-wont-load
		sb.append("<img src=\""
			+AboutDialog.class.getClassLoader().getResource(
			"resources/images/wavegraph_about_screen.png"
			).toString()+"\"/>");
		sb.append("<br>");
		sb.append(BuildInfo.getGitCommit()+" (V "+g.progVersion+")</p>");

		sb.append("<p><strong>"+g.progName+"</strong></p>");
		sb.append("<h2>"+"Visualize RIFF WAVE Files"+"</h2>");
/*

		sb.append("<p><strong>"+l.tr("Credits & Program Libraries")+"</strong></p>");
		sb.append("<table>");// width=\"400px\">");
		sb.append("<tr>");

		sb.append("<td>JACK</td><td>"+ahref("http://www.jackaudio.org")+"</td>");
		sb.append("</tr><tr>");

		sb.append("<td>liblo</td><td>"+ahref("http://liblo.sourceforge.net")+"</td>");
		sb.append("</tr><tr>");

		sb.append("<td>JavaOSC</td><td>"+ahref("https://github.com/hoijui/JavaOSC")+"</td>");
		sb.append("</tr><tr>");

		sb.append("<td>gettext</td><td>"+ahref("https://code.google.com/p/gettext-commons")+"</td>");
		sb.append("</tr><tr>");

		sb.append("<td>OpenJDK</td><td>"+ahref("http://openjdk.java.net")+"</td>");

		sb.append("</tr>");
		sb.append("</table>");
*/
		sb.append("<p><strong>"+ahref(g.progHome)+"</strong></p>");

		sb.append("<p>© 2015 Thomas Brand &lt;tom@trellis.ch&gt;</p>");
		sb.append("<p><small>This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 2 or later.</small></p>");

		sb.append("</body></html>");
		return sb.toString();
	}//end getAboutHtml
}//end class AboutDialog
