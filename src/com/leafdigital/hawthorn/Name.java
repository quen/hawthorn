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
package com.leafdigital.hawthorn;

/** Represents a user name in a channel. */
public class Name
{
	private String user,displayName;

	/**
	 * @param user User ID
	 * @param displayName Display name
	 */
	public Name(String user, String displayName)
	{
		this.user = user;
		this.displayName = displayName;
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

	/** @return JavaScript version of name object */
	public String getJSFormat()
	{
		return "{user:'"+user+"',displayName:'"+displayName+"'}";
	}
}
