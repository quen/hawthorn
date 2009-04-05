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
