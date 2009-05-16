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

/**
 * Message sent when somebody leaves a channel. This message may be sent by a
 * user (for example when they close a popup window); if not, the server
 * generates it after the user hasn't acted in the channel for a short time.
 */
public class LeaveMessage extends Message
{
	/** Type of message */
	private final static String TYPE = "LEAVE";

	static void init() throws SecurityException, NoSuchMethodException
	{
		Message.registerType(TYPE, LeaveMessage.class);
	}

	private boolean timeout;

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param userMasked Masked version of user name, for untrusted recipients
	 * @param displayName Display name of user
	 * @param extra Extra user data
	 * @param timeout True if it's a timeout, false if user requested it
	 */
	LeaveMessage(long time, String channel, String ip, String user,
		String userMasked, String displayName, String extra, boolean timeout)
	{
		super(time, channel, ip, user, userMasked, displayName, extra);
		this.timeout = timeout;
	}

	@Override
	protected String getAdditionalJS(boolean trusted)
	{
		return ",timeout:" + timeout;
	}

	@Override
	protected String getAdditionalLog()
	{
		return timeout ? " timeout" : " explicit";
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
	 */
	public static LeaveMessage parseMessage(long time, String channel, String ip,
		String user, String displayName, String extra, String additional,
		Hawthorn app)
	{
		boolean timeout = additional.equals("timeout");
		if(!timeout && !additional.equals("explicit"))
		{
			throw new IllegalArgumentException(
				"Extra text must be timeout or explicit");
		}
		return new LeaveMessage(time, channel, ip, user, app.getMaskedUser(user),
			displayName, extra, timeout);
	}

}
