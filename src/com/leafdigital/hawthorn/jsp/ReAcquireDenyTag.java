package com.leafdigital.hawthorn.jsp;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

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
		getJspContext().getOut().println("hawthorn.reAcquireDeny('"+
			id+"','"+error+"');");
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
