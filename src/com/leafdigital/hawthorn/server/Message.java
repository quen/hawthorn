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

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.regex.*;

import com.leafdigital.hawthorn.util.JS;

/** A single message sent on a channel. */
public abstract class Message
{
	private final static Pattern COMMAND =
		Pattern.compile("([A-Z]+) (" + Hawthorn.REGEXP_USERCHANNEL
			+ ") ([0-9a-f:.]+) (" + Hawthorn.REGEXP_USERCHANNEL + ") \"("
			+ Hawthorn.REGEXP_DISPLAYNAME + ")\" \"(" + Hawthorn.REGEXP_EXTRA
			+ ")\"(.*)");

	private static HashMap<String, Method> messageInitMethods =
		new HashMap<String, Method>();

	private long time;

	private String channel, user, userMasked, displayName, extra, ip;

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User ID who sent message
	 * @param userMasked Masked version of user ID, for untrusted recipients
	 * @param displayName Display name of user
	 * @param extra Extra per-user data
	 */
	Message(long time, String channel, String ip, String user, String userMasked,
		String displayName, String extra)
	{
		this.time = time;
		this.channel = channel;
		this.ip = ip;
		this.user = user;
		this.userMasked = userMasked;
		this.displayName = displayName;
		this.extra = extra;
	}

	/** @return Time of message */
	public long getTime()
	{
		return time;
	}

	/** @return Channel name */
	public String getChannel()
	{
		return channel;
	}

	/** @return User who sent message */
	public String getUser()
	{
		return user;
	}

	/** @return Display name of user */
	public String getDisplayName()
	{
		return displayName;
	}

	/** @return Extra user data */
	public String getExtra()
	{
		return extra;
	}

	/** @return IP address of user */
	public String getIP()
	{
		return ip;
	}

	/**
	 * @param trusted True if user gets to see real user ID etc
	 * @return JS version of message (not including channel as this is known)
	 */
	public String getJSFormat(boolean trusted)
	{
		return "{type:'" + getType() + "',time:" + time + ",user:'"
		  + (trusted ? user : userMasked)	+ "',displayName:'" + JS.esc(displayName)
		  + "',extra:'" + JS.esc(extra) + "'" + getAdditionalJS(trusted) + "}";
	}

	/**
	 * @return Version of message to put in logs (not including channel as logfile
	 *   defines that, or time which is added by logger)
	 */
	public String getLogFormat()
	{
		return getType() + ' ' + ip + ' ' + user + " \"" + displayName
			+  "\" \"" + extra + "\"" + getAdditionalLog();
	}

	/** @return Version of message that will be sent to other servers */
	public String getServerFormat()
	{
		return getType() + " " + channel + " " + ip + " " + user + " \""
			+ displayName + "\" \"" + extra + "\"" + getAdditionalLog();
	}

	/**
	 * @param trusted True if user gets to see real user ID etc
	 * @return Additional data to go into the JavaScript message representation
	 */
	protected abstract String getAdditionalJS(boolean trusted);

	/** @return Additional data to go into server/log message representations */
	protected abstract String getAdditionalLog();

	/** @return Type of message, used as command string for server-server messages */
	public abstract String getType();

	/**
	 * Sets time. Used only when adjusting time for uniqueness within channel.
	 *
	 * @param time New time
	 */
	void setTime(long time)
	{
		this.time = time;
	}

	/**
	 * Called when initialising other message types.
	 *
	 * @param command Server-to-server command name
	 * @param cl Class implementing message
	 * @throws NoSuchMethodException Error obtaining the parseMessage method
	 * @throws SecurityException Error obtaining the parseMessage method
	 */
	static void registerType(String command, Class<? extends Message> cl)
		throws SecurityException, NoSuchMethodException
	{
		Method m = cl.getMethod("parseMessage", long.class, String.class,
			String.class, String.class, String.class, String.class, String.class,
			Hawthorn.class);
		messageInitMethods.put(command, m);
	}

	/**
	 * Constructs a new message based on a server-to-server line.
	 *
	 * @param line Line of text (not including \n)
	 * @param app Hawthorn app object
	 * @return New message
	 * @throws IllegalArgumentException If it isn't a valid message line
	 * @throws InvocationTargetException If there's an error invoking the init
	 *         method inside the message subclass
	 * @throws IllegalAccessException Access failure
	 */
	public static Message parseMessage(String line, Hawthorn app)
		throws IllegalArgumentException, IllegalAccessException,
		InvocationTargetException
	{
		Matcher m = COMMAND.matcher(line);
		if(!m.matches())
		{
			throw new IllegalArgumentException("Line not valid: " + line);
		}

		Method parseMessage = messageInitMethods.get(m.group(1));
		if(parseMessage == null)
		{
			throw new IllegalArgumentException("Unknown command: " + m.group(1));
		}

		return (Message)parseMessage.invoke(null, System.currentTimeMillis(),
			m.group(2), m.group(3), m.group(4), m.group(5), m.group(6), m.group(7),
			app);
	}
}
