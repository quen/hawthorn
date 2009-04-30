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

/** Message sent when somebody says something. */
public class BanMessage extends UniqueMessage
{
	/** Type of message */
	private final static String TYPE = "BAN";

	static
	{
		try
		{
			Message.registerType(TYPE, BanMessage.class);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	private final static Pattern REGEXP_EXTRA = Pattern.compile("^(" +
		Hawthorn.REGEXP_USERCHANNEL + ") (" +	HttpEvent.REGEXP_LONG +
		")}([0-9]+)$");

	private String ban;
	private long until;

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param displayName Display name of user
	 * @param unique A unique identifier (within channel and user) to avoid
	 *   possibility of duplicated messages
	 * @param ban User ID being banned
	 * @param until Time they're banned until
	 */
	BanMessage(long time, String channel, String ip, String user,
		String displayName, String unique, String ban, long until)
	{
		super(time, channel, ip, user, displayName, unique);
		this.ban = ban;
		this.until = until;
	}

	/** @return Banned user ID */
	public String getBan()
	{
		return ban;
	}

	/** @return Time user is banned until */
	public long getUntil()
	{
		return until;
	}

	@Override
	protected String getExtraJS()
	{
		return ",ban:'" + ban + "',until:"+until;
	}

	@Override
	protected String getExtra()
	{
		return " " + ban + " " + until;
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
	public static BanMessage parseMessage(long time, String channel, String ip,
		String user, String displayName, String extra)
		throws IllegalArgumentException
	{
		Matcher m = REGEXP_EXTRA.matcher(extra);
		if(!m.matches())
		{
			throw new IllegalArgumentException("Unexpected 'extra' value");
		}

		return new BanMessage(time, channel, ip, user, displayName, m.group(3),
			m.group(1), Long.parseLong(m.group(2)));
	}

}
