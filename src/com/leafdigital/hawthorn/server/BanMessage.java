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
public class BanMessage extends UniqueMessage
{
	/** Type of message */
	private final static String TYPE = "BAN";

	static void init() throws SecurityException, NoSuchMethodException
	{
		Message.registerType(TYPE, BanMessage.class);
	}

	private final static Pattern REGEXP_ADDITIONAL = Pattern.compile("^("
		+ Hawthorn.REGEXP_USERCHANNEL + ") \"(" + Hawthorn.REGEXP_DISPLAYNAME
		+ ")\" \"(" + Hawthorn.REGEXP_EXTRA + ")\" ("
		+ HttpEvent.REGEXP_LONG + ")}([0-9]+)$");

	private String ban, banMasked, banDisplayName, banExtra;
	private long until;

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param userMasked Masked version of user name, for untrusted recipients
	 * @param displayName Display name of user
	 * @param extra Extra user data
	 * @param unique A unique identifier (within channel and user) to avoid
	 *   possibility of duplicated messages
	 * @param ban User name being banned
	 * @param banMasked Masked version of user name, for untrusted recipients
	 * @param banDisplayName Possible display name of user (note: this is
	 *   not used to identify the user, only to display information about the
	 *   ban to other users; it can be any text)
	 * @param banExtra Possible extra user data (also not used to identify)
	 * @param until Time they're banned until
	 */
	BanMessage(long time, String channel, String ip, String user,
		String userMasked, String displayName, String extra, String unique,
		String ban, String banMasked, String banDisplayName, String banExtra,
		long until)
	{
		super(time, channel, ip, user, userMasked, displayName, extra, unique);
		this.ban = ban;
		this.banMasked = banMasked;
		this.banDisplayName = banDisplayName;
		this.banExtra = banExtra;
		this.until = until;
	}

	/** @return Banned user name */
	public String getBan()
	{
		return ban;
	}

	/** @return Banned user's display name */
	public String getBanDisplayName()
	{
		return banDisplayName;
	}

	/** @return Banned user's extra data */
	public String getBanExtra()
	{
		return banExtra;
	}

	/** @return Time user is banned until */
	public long getUntil()
	{
		return until;
	}

	@Override
	protected String getAdditionalJS(boolean trusted)
	{
		return ",ban:'" + (trusted ? ban : banMasked) + "',banDisplayName:'"
			+ JS.esc(banDisplayName) + "',banExtra:'" + JS.esc(banExtra)
			+ "',until:" + until;
	}

	@Override
	protected String getAdditionalLog()
	{
		return " " + ban + " \"" + banDisplayName + "\" \"" + banExtra
			+ "\" " + until;
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
	 * @param extra Extra user data
	 * @param additional Bit that goes after all this in the text
	 * @param app Hawthorn app object
	 * @return New message
	 * @throws IllegalArgumentException If the 'extra' value does not match
	 *   expected pattern
	 */
	public static BanMessage parseMessage(long time, String channel, String ip,
		String user, String displayName, String extra, String additional,
		Hawthorn app)
		throws IllegalArgumentException
	{
		Matcher m = REGEXP_ADDITIONAL.matcher(additional);
		if(!m.matches())
		{
			throw new IllegalArgumentException("Unexpected 'extra' value");
		}

		return new BanMessage(time, channel, ip, user, app.getMaskedUser(user),
			displayName, extra, m.group(5), m.group(1), app.getMaskedUser(m.group(1)),
			m.group(2), m.group(3), Long.parseLong(m.group(4)));
	}

}
