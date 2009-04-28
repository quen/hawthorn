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
package com.leafdigital.hawthorn.server;

import java.util.regex.*;

import com.leafdigital.hawthorn.util.JS;

/** Message sent when somebody says something. */
public class SayMessage extends Message
{
	/** Type of message */
	private final static String TYPE = "SAY";

	static
	{
		try
		{
			Message.registerType(TYPE, SayMessage.class);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	private final static Pattern REGEXP_EXTRA = Pattern.compile("^(.*)}([0-9]+)$");

	private String message, unique;

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param displayName Display name of user
	 * @param message Message text
	 * @param unique A unique identifier (within channel and user) to avoid
	 *   possibility of duplicated messages
	 */
	SayMessage(long time, String channel, String ip, String user,
		String displayName, String message, String unique)
	{
		super(time, channel, ip, user, displayName);
		this.message = message;
		this.unique = unique;
	}

	/** @return Message text */
	public String getMessage()
	{
		return message;
	}

	/** @return Unique identifier (within channel and user) to avoid duplicates */
	public String getUnique()
	{
		return unique;
	}

	@Override
	protected String getExtraJS()
	{
		return ",text:'" + JS.esc(message) + "'";
	}

	@Override
	protected String getExtra()
	{
		return " " + message;
	}

	@Override
	public String getServerFormat()
	{
		return super.getServerFormat() + "}" + unique;
	}

	@Override
	public String getType()
	{
		return TYPE;
	}

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param displayName Display name of user
	 * @param extra Bit that goes after all this in the text
	 * @return New message
	 * @throws IllegalArgumentException If the 'extra' value does not match
	 *   expected pattern
	 */
	public static SayMessage parseMessage(long time, String channel, String ip,
		String user, String displayName, String extra)
		throws IllegalArgumentException
	{
		Matcher m = REGEXP_EXTRA.matcher(extra);
		if(!m.matches())
		{
			throw new IllegalArgumentException("Unexpected 'extra' value");
		}

		return new SayMessage(time, channel, ip, user, displayName, m.group(1),
			m.group(2));
	}

}
