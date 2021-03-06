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
public class SayMessage extends UniqueMessage
{
	/** Type of message */
	private final static String TYPE = "SAY";

	static void init() throws SecurityException, NoSuchMethodException
	{
		Message.registerType(TYPE, SayMessage.class);
	}

	private final static Pattern REGEXP_ADDITIONAL =
		Pattern.compile("^(.*)}([0-9]+)$");

	private String message;

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
	 * @param message Message text
	 */
	SayMessage(long time, String channel, String ip, String user,
		String userMasked, String displayName, String extra, String unique,
		String message)
	{
		super(time, channel, ip, user, userMasked, displayName, extra, unique);
		this.message = message;
	}

	/** @return Message text */
	public String getMessage()
	{
		return message;
	}

	@Override
	protected String getAdditionalJS(boolean trusted)
	{
		return ",text:'" + JS.esc(message) + "'";
	}

	@Override
	protected String getAdditionalLog()
	{
		return " " + message;
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
	public static SayMessage parseMessage(long time, String channel, String ip,
		String user, String displayName, String extra, String additional,
		Hawthorn app)
		throws IllegalArgumentException
	{
		Matcher m = REGEXP_ADDITIONAL.matcher(additional);
		if(!m.matches())
		{
			throw new IllegalArgumentException("Unexpected 'extra' value");
		}

		return new SayMessage(time, channel, ip, user, app.getMaskedUser(user),
			displayName, extra, m.group(2), m.group(1));
	}

}
