/*
Copyright 2009 Samuel Marshall
http://www.leafdigital.com/software/hawthorn/

This file is part of hawthorn.

hawthorn is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

hawthorn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with hawthorn.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.leafdigital.hawthorn;

/** A single message */
public class Message
{
	private long time;
	private String channel;
	private String user;
	private String displayName;
	private String message;
	private String ip;
	
	/**
	 * @param time Time of message
	 * @param channel Channel of message
	 * @param ip IP address of user
	 * @param user User who sent message
	 * @param displayName Display name of user
	 * @param message Message text
	 */
	Message(long time,String channel,String ip,String user,String displayName,String message)
	{
		this.time=time;
		this.channel=channel;
		this.ip=ip;
		this.user=user;
		this.displayName=displayName;
		this.message=message;
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

	/** @return Message text */
	public String getMessage()
	{
		return message;
	}
	
	/** @return IP address of user */
	public String getIP()
	{
		return ip;
	}
	
	/** @return JS version of message (not including channel as this is known) */
	public String getJS()
	{
		StringBuilder result=new StringBuilder();
		result.append("{time:");
		result.append(time);
		result.append(",user:'");
		result.append(user); // Not permitted to contain any weird characters
		result.append("',displayname:'");
		result.append(Hawthorn.escapeJS(displayName));
		result.append("',text:'");
		result.append(Hawthorn.escapeJS(message));
		result.append("'}");
		return result.toString();
	}

	/** @param time New time */
	public void setTime(long time)
	{
		this.time=time;
	}
}
