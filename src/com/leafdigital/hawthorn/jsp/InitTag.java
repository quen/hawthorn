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

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.leafdigital.hawthorn.util.*;

/** JSP tag that sets up shared Hawthorn variables. */
public class InitTag extends BodyTagSupport
{
	/** Attribute used on page context. */
	static final String HAWTHORN_INIT_TAG = "hawthorn.initTag";
	private static final long DEFAULT_KEY_EXPIRY = 60*60*1000;

	private String magicNumber, user, displayName, jsUrl, popupUrl, reAcquireUrl;
	private long keyExpiry = DEFAULT_KEY_EXPIRY;
	private URL[] servers;
	private boolean defer;

	@Override
	public int doEndTag() throws JspException
	{
		// Store tag reference in context
		if (pageContext.getAttribute(HAWTHORN_INIT_TAG) != null)
		{
			throw new JspException(
				"Page may not contain more than one Hawthorn <init> tag");
		}
		pageContext.setAttribute(HAWTHORN_INIT_TAG, this);

		// Get server list from content
		if(getBodyContent()==null)
		{
			throw new JspException("<init> must contain server list.");
		}

		String content = getBodyContent().getString();

		// Parse list
		Document doc;
		try
		{
			doc =	XML.getDocumentBuilder().parse(new ByteArrayInputStream(
				("<?xml version='1.0' encoding='UTF-8' ?><servers>" + content
					+ "</servers>").getBytes("UTF-8")));
		}
		catch (IOException e)
		{
			throw new JspException("<init> content is not valid XML.");
		}
		catch (SAXException e)
		{
			throw new JspException("<init> content is not valid XML.");
		}
		catch (ParserConfigurationException e)
		{
			throw new JspException("<init> content is not valid XML.");
		}

		// Check list
		LinkedList<URL> serverList = new LinkedList<URL>();
		for (Node n = doc.getDocumentElement().getFirstChild(); n != null;
			n =	n.getNextSibling())
		{
			if(n instanceof Element)
			{
				Element e = (Element)n;
				if (!e.getTagName().equals("server"))
				{
					throw new JspException("Unexpected content in <init>: <" +
						e.getTagName() + ">");
				}

				String serverName = getText(e);
				try
				{
					URL u=new URL(serverName);
					serverList.add(u);
				}
				catch (MalformedURLException e1)
				{
					throw new JspException("Not a valid server URL: " + serverName);
				}
			}
		}
		servers=serverList.toArray(new URL[serverList.size()]);

		return EVAL_PAGE;
	}

	/**
	 * Obtains text from inside an XML element.
	 *
	 * @param container Containing element
	 * @return Text string (will have whitespace trimmed)
	 * @throws JspException If there is anything other than text in there
	 */
	private static String getText(Element container) throws JspException
	{
		try
		{
			return XML.getText(container);
		}
		catch(IOException e)
		{
			throw new JspException(e.getMessage());
		}
	}

	/**
	 * @param magicNumber Magic number used by Hawthorn servers to validate
	 *        authentication.
	 */
	public void setMagicNumber(String magicNumber)
	{
		this.magicNumber = magicNumber;
	}

	/**
	 * @param user Hawthorn user ID
	 */
	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * @param displayName Hawthorn user display name
	 */
	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

	/**
	 * @param jsUrl URL to hawthorn.js from this page (may be relative)
	 */
	public void setJsUrl(String jsUrl)
	{
		this.jsUrl = jsUrl;
	}

	/**
	 * @param popupUrl URL to popup.html from this page (may be relative)
	 */
	public void setPopupUrl(String popupUrl)
	{
		this.popupUrl = popupUrl;
	}


	/** @param reAcquireUrl URL to JSP script that acquires a new access key */
	public void setReAcquireUrl(String reAcquireUrl)
	{
		this.reAcquireUrl = reAcquireUrl;
	}

	/** @param keyExpiry Expiry time for generated keys in milliseconds */
	public void setKeyExpiry(long keyExpiry)
	{
		this.keyExpiry = keyExpiry;
	}

	/**
	 * Gets an authentication key based on the known data. Uses the inited user
	 * and display name.
	 *
	 * @param channel Channel ID
	 * @param keyTime Key time
	 * @return Key
	 * @throws JspException
	 */
	String getKey(String channel, long keyTime) throws JspException
	{
		return getKey(channel, keyTime, user, displayName);
	}

	/**
	 * Gets an authentication key based on the known data.
	 *
	 * @param channel Channel ID
	 * @param keyTime Key time
	 * @param overrideUser User ID
	 * @param overrideDisplayName User display name
	 * @return Key
	 * @throws JspException
	 */
	String getKey(String channel, long keyTime, String overrideUser,
		String overrideDisplayName) throws JspException
	{
		try
		{
			return Auth.getKey(magicNumber, overrideUser, overrideDisplayName,
				channel, keyTime);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new JspException("Java installation does not appear to include "
				+ "SHA-1 hash facilities. Correct installation before trying to use "
				+ "Hawthorn.", e);
		}
	}

	/** @return Hawthorn user ID */
	public String getUser()
	{
		return user;
	}

	/** @return Hawthorn user displayed name */
	public String getDisplayName()
	{
		return displayName;
	}

	/** @return URL (possibly relative) to popup.html */
	public String getPopupUrl()
	{
		return popupUrl;
	}

	/** @return URL (absolute or relative to popup.html) to re-acquire script */
	public String getReAcquireUrl()
	{
		return reAcquireUrl;
	}

	/** @return Server list */
	public URL[] getServers()
	{
		URL[] serversCopy = new URL[servers.length];
		System.arraycopy(servers, 0, serversCopy, 0, servers.length);
		return serversCopy;
	}

	/** @param defer True to defer JavaScript */
	public void setDefer(boolean defer)
	{
		this.defer = defer;
	}

	/** @return True if JavaScript inclusion is deferred */
	boolean getDefer()
	{
		return defer;
	}

	/** @return Expiry time for generated keys in milliseconds */
	public long getKeyExpiry()
	{
		return keyExpiry;
	}

	/**
	 * Prints script tag unless defer is set.
	 * @param out Writer
	 * @param evenDeferred If true, prints even if deferred
	 * @throws IOException If error writing tag to output
	 */
	public void printJS(JspWriter out, boolean evenDeferred) throws IOException
	{
		if((!defer || evenDeferred) && pageContext.getAttribute("hawthorn.printedJS")==null)
		{
			pageContext.setAttribute("hawthorn.printedJS",Boolean.TRUE);
			out.println("<script type='text/javascript' src='"
				+XML.esc(jsUrl)+"'></script>");
			out.println("<script type='text/javascript'>");
			out.print("hawthorn.init([");
			boolean first=true;
			for(URL server : getServers())
			{
				if(first)
				{
					first=false;
					out.print('\'');
				}
				else
				{
					out.print(",'");
				}
				out.print(server);
				out.print('\'');
			}
			out.println("]);");
			out.println("</script>");
		}
	}
}
