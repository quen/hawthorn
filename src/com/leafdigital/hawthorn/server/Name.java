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
	private String user,displayName, ip;

	/**
	 * @param user User ID
	 * @param displayName Display name
	 * @param ip IP address
	 */
	public Name(String user, String displayName, String ip)
	{
		this.user = user;
		this.displayName = displayName;
		this.ip = ip;
	}

	/** @return User ID */
	public String getUser()
	{
		return user;
	}

	/** @return User display name */
	public String getDisplayName()
	{
		return displayName;
	}

	/** @return IP address of users */
	public String getIP()
	{
		return ip;
	}

	/** @return JavaScript version of name object */
	public String getJSFormat()
	{
		// Does not include IP; we don't send that to users
		return "{user:'" + user + "',displayName:'" + JS.esc(displayName) + "'}";
	}
}
