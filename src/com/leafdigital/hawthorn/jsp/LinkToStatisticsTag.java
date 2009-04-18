package com.leafdigital.hawthorn.jsp;

import java.io.IOException;
import java.net.URL;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.leafdigital.hawthorn.util.*;

/** Tag displays recent messages and users on a Hawthorn chat channel. */
public class LinkToStatisticsTag extends SimpleTagSupport
{
	@Override
	public void doTag() throws JspException, IOException
	{
		// Get init tag with basic settings
		InitTag init =
			(InitTag)getJspContext().getAttribute(InitTag.HAWTHORN_INIT_TAG);
		if (init==null)
		{
			throw new JspException("Cannot use <linkToStatistics> without <init>");
		}

		// Get auth code
		long time = System.currentTimeMillis() + init.getKeyExpiry();
		String key = init.getKey("!system", time, "_admin", "_");

		// Output list
		getJspContext().getOut().println("<ul class='hawthorn_statslinks'>");
		URL[] servers = init.getServers();
		for(URL server : servers)
		{
			getJspContext().getOut().println(
				"<li><a href='"+XML.esc(server.toString())+"hawthorn/html/statistics"
				+ "?channel=!system&amp;user=_admin&amp;displayname=_&amp;keytime="
				+ time + "&amp;key=" + key + "'>"
				+ XML.esc(server.toString()) + "</a></li>");
		}
	}
}
