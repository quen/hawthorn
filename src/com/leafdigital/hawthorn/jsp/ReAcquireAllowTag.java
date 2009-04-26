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

/** Tag prints JavaScript for a successful key re-acquire. */
public class ReAcquireAllowTag extends SimpleTagSupport
{
	private String channel,id;

	@Override
	public void doTag() throws JspException, IOException
	{
		// Get init tag with basic settings
		InitTag init =
			(InitTag)getJspContext().getAttribute(InitTag.HAWTHORN_INIT_TAG);
		if (init==null)
		{
			throw new JspException("Cannot use <reAcquireAllow> without <init>");
		}

		// Get auth code
		long time = System.currentTimeMillis() + init.getKeyExpiry();
		String key = init.getKey(channel,time);

		// Print JavaScript
		getJspContext().getOut().println("hawthorn.reAcquireComplete('"+
			id+"','"+key+"','"+time+"');");
	}

	/** @param channel Hawthorn channel ID */
	public void setChannel(String channel)
	{
		this.channel = channel;
	}

	/** @param id JavaScript ID */
	public void setId(String id)
	{
		this.id = id;
	}
}
