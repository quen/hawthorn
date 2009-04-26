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

import com.leafdigital.hawthorn.util.JS;

/** Tag prints JavaScript for a failed key re-acquire. */
public class ReAcquireDenyTag extends SimpleTagSupport
{
	private String error,id;

	@Override
	public void doTag() throws JspException, IOException
	{
		// Get init tag with basic settings
		InitTag init =
			(InitTag)getJspContext().getAttribute(InitTag.HAWTHORN_INIT_TAG);
		if (init==null)
		{
			throw new JspException("Cannot use <reAcquireDeny> without <init>");
		}

		// Print JavaScript
		getJspContext().getOut().println("hawthorn.reAcquireDeny('" +
			id+"','" + JS.escapeJS(error) + "');");
	}

	/** @param error Error message */
	public void setError(String error)
	{
		this.error = error;
	}

	/** @param id JavaScript ID */
	public void setId(String id)
	{
		this.id = id;
	}
}
