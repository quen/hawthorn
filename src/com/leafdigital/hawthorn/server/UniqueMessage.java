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

/** Messages which are checked for uniqueness. */
public abstract class UniqueMessage extends Message
{
	private String unique;

	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param displayName Display name of user
	 * @param unique A unique identifier (within channel and user) to avoid
	 *   possibility of duplicated messages
	 */
	UniqueMessage(long time, String channel, String ip, String user,
		String displayName, String unique)
	{
		super(time, channel, ip, user, displayName);
		this.unique = unique;
	}

	/** @return Unique identifier (within channel and user) to avoid duplicates */
	public String getUnique()
	{
		return unique;
	}

	@Override
	public String getServerFormat()
	{
		return super.getServerFormat() + "}" + unique;
	}
}
