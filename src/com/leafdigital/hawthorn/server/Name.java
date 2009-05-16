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

import com.leafdigital.hawthorn.util.JS;

/** Represents a user name in a channel. */
public class Name
{
	private String user, userMasked, displayName, extra, ip;

	/**
	 * @param user User name
	 * @param userMasked Masked version of user name, for untrusted recipients
	 * @param displayName Display name
	 * @param extra Extra per-user data
	 * @param ip IP address
	 */
	public Name(String user, String userMasked, String displayName, String extra,
		String ip)
	{
		this.user = user;
		this.userMasked = userMasked;
		this.displayName = displayName;
		this.extra = extra;
		this.ip = ip;
	}

	/** @return User name */
	public String getUser()
	{
		return user;
	}

	/** @return User display name */
	public String getDisplayName()
	{
		return displayName;
	}

	/** @return Extra user data */
	public String getExtra()
	{
		return extra;
	}

	/** @return IP address of users */
	public String getIP()
	{
		return ip;
	}

	/**
	 * @param trusted True if user gets to see real user name etc
	 * @return JavaScript version of name object
	 */
	public String getJSFormat(boolean trusted)
	{
		// Does not include IP; we don't send that to users, even trusted ones
		return "{user:'" + (trusted ? user : userMasked) + "',displayName:'"
			+ JS.esc(displayName) + "'}";
	}
}
