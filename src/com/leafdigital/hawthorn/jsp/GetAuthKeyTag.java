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

/** Tag obtains an authorisation key for a Hawthorn chat channel. */
public class GetAuthKeyTag extends SimpleTagSupport
{
	private String channel;

	@Override
	public void doTag() throws JspException, IOException
	{
		// Get init tag with basic settings
		InitTag init =
			(InitTag)getJspContext().getAttribute(InitTag.HAWTHORN_INIT_TAG);
		if (init==null)
		{
			throw new JspException("Cannot use <getAuthKey> without <init>");
		}

		// Get auth code
		long time = System.currentTimeMillis() + init.getKeyExpiry();
		String key = init.getKey(channel,time);

		// Set variables
	  getJspContext().setAttribute("hawthornKey",key);
	  getJspContext().setAttribute("hawthornKeyTime",time);
	}

	/** @param channel Hawthorn channel ID */
	public void setChannel(String channel)
	{
		this.channel = channel;
	}
}
