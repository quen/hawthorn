/*
Copyright 2009 Samuel Marshall
http://www.leafdigital.com/software/hawthorn/

This file is part of Hawthorn.

Hawthorn is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Hawthorn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Hawthorn.  If not, see <http://www.gnu.org/licenses/>.
*/
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
		boolean usedRecent = getJspContext().getAttribute(
			RecentTag.HAWTHORN_GET_RECENT_COUNT) != null;
		boolean usedLinkToChat = getJspContext().getAttribute(
			LinkToChatTag.HAWTHORN_LINK_TO_CHAT_COUNT) != null;
		if(!usedRecent && !usedLinkToChat)
		{
			return;
		}

		// Print script tag
		init.printJS(getJspContext().getOut(),true);

		// Run all the deferred recent tags
		if(usedRecent)
		{
			getJspContext().getOut().println("<script type='text/javascript'>");
			getJspContext().getOut().println(
				"for(var i=0;i<hawthorn_recent.length;i++)\n" +
				"{\n" +
				"\thawthorn.handleRecent(hawthorn_recent[i]);\n" +
				"}");
			getJspContext().getOut().println("</script>");
		}
	}
}
