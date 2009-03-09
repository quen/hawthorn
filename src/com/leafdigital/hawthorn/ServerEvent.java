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

import java.lang.reflect.InvocationTargetException;

import com.leafdigital.hawthorn.HttpServer.Connection;
import com.leafdigital.hawthorn.Logger.Level;

/** Handles events generated from a remote server. */
public class ServerEvent extends Event
{
	private String request;

	private Connection connection;

	/**
	 * @param app Main app object
	 * @param request Server request string
	 * @param connection Connection from remote server
	 */
	public ServerEvent(Hawthorn app, String request, Connection connection)
	{
		super(app);
		this.request = request;
		this.connection = connection;
	}

	@Override
	public void handle() throws OperationException
	{
		try
		{
			Message m = Message.parseMessage(request);
			getLogger().log(Logger.SYSTEMLOG, Logger.Level.DETAIL,
				"Received from remote: " + request);
			getChannels().get(m.getChannel()).message(m, true);
		}
		catch (IllegalArgumentException e)
		{
			fail();
		}
		catch (IllegalAccessException e)
		{
			fail();
		}
		catch (InvocationTargetException e)
		{
			fail();
		}
	}

	private void fail()
	{
		// Unsupported request
		getLogger().log(Logger.SYSTEMLOG, Level.ERROR,
			"Unexpected line from server " + connection + ": " + request);
		connection.close();
	}
}
