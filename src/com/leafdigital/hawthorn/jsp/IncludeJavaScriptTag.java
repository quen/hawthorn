package com.leafdigital.hawthorn.jsp;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/** Tag adds JavaScript file if it was deferred (and if required). */
public class IncludeJavaScriptTag extends SimpleTagSupport
{
	@Override
	public void doTag() throws JspException, IOException
	{
		// Get init tag with basic settings
		InitTag init =
			(InitTag)getJspContext().getAttribute(InitTag.HAWTHORN_INIT_TAG);
		if (init==null)
		{
			throw new JspException("Cannot use <includeJavaScript> without <init>");
		}

		// If not deferred, do nothing
		if (!init.getDefer())
		{
			return;
		}

		// If no relevant tags, do nothing
		boolean usedGetRecent = getJspContext().getAttribute(
			GetRecentTag.HAWTHORN_GET_RECENT_COUNT)	!= null;
		boolean usedLinkToChat = getJspContext().getAttribute(
			LinkToChatTag.HAWTHORN_LINK_TO_CHAT_COUNT) != null;
		if(!usedGetRecent && !usedLinkToChat)
		{
			return;
		}

		// Print script tag
		init.printJS(getJspContext().getOut(),true);

		// Run all the deferred getRecent tags
		if(usedGetRecent)
		{
			getJspContext().getOut().println("<script type='text/javascript'>");
			getJspContext().getOut().println(
				"for(var i=0;i<hawthorn_getrecent.length;i++)\n" +
				"{\n" +
				"\thawthorn.handleGetRecent(hawthorn_getrecent[i]);\n" +
				"}");
			getJspContext().getOut().println("</script>");
		}
	}
}