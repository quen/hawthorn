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
