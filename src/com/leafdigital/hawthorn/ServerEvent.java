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

import java.util.regex.*;

import com.leafdigital.hawthorn.HttpServer.Connection;
import com.leafdigital.hawthorn.Logger.Level;

/** Handles events generated from a remote server. */
public class ServerEvent extends Event
{
	private String request;
	private Connection connection;
	
	private final static Pattern SAY=Pattern.compile(
		"SAY ("+Hawthorn.REGEXP_USERCHANNEL+") ([0-9a-f:.]+) ("+Hawthorn.REGEXP_USERCHANNEL+
		") \"("+Hawthorn.REGEXP_DISPLAYNAME+")\" ("+Hawthorn.REGEXP_MESSAGE+")");

	/**
	 * @param app Main app object
	 * @param request Server request string
	 * @param connection Connection from remote server
	 */
	public ServerEvent(Hawthorn app,String request,Connection connection)
	{
		super(app);
		this.request=request;
		this.connection=connection;
	}

	@Override
	public void handle() throws OperationException
	{
		Matcher m=SAY.matcher(request);
		if(m.matches())
		{
			getLogger().log(Logger.SYSTEMLOG,Logger.Level.DETAIL,
				"Received from remote: "+request);
			handleSay(m.group(1),m.group(2),m.group(3),m.group(4),m.group(5));
		}
		else
		{
			// Unsupported request
			getLogger().log(Logger.SYSTEMLOG,Level.ERROR,
				"Unexpected line from server "+connection+": "+request);
			connection.close();			
		}
	}
	
	private void handleSay(String channel,String ip,String user,String displayName,
		String message)
	{
		getChannels().get(channel).say(new Message(System.currentTimeMillis(),
			channel,ip,user,displayName,message));
	}
}
