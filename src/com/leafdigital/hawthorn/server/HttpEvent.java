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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import com.leafdigital.hawthorn.util.*;

/** Event generated by HTTP request. Handles the user's request. */
public class HttpEvent extends Event
{
	/** Regular expression matching positive longs */
	private static final String REGEXP_LONG = "[0-9]{1,18}";

	/** Regular expression matching positive ints */
	private static final String REGEXP_INT = "[0-9]{1,9}";

	private String request;

	private HttpServer.Connection connection;

	private long requestTime;

	/**
	 * @param app Main app object
	 * @param request HTTP request path
	 * @param connection Connection object that made this request
	 */
	HttpEvent(Hawthorn app, String request, HttpServer.Connection connection)
	{
		super(app);
		this.request = request;
		this.connection = connection;
		requestTime = System.currentTimeMillis();
	}

	@Override
	public void handle() throws OperationException
	{
		boolean html=false;
		try
		{
			String path;
			HashMap<String, String> params = new HashMap<String, String>();

			int question = request.indexOf('?');
			if (question == -1)
			{
				path = request;
			}
			else
			{
				path = request.substring(0, question);
				String remainder = request.substring(question + 1);
				while (remainder.length() > 0)
				{
					int and = remainder.indexOf('&');
					String paramPair;
					if (and == -1)
					{
						paramPair = remainder;
						remainder = "";
					}
					else
					{
						paramPair = remainder.substring(0, and);
						remainder = remainder.substring(and + 1);
					}

					int equals = paramPair.indexOf('=');
					if (equals == -1)
					{
						params.put(paramPair, null);
					}
					else
					{
						try
						{
							String value = paramPair.substring(equals + 1);
							params.put(paramPair.substring(0, equals), URLDecoder.decode(
								value, "UTF-8"));
						}
						catch (UnsupportedEncodingException e)
						{
							throw new Error("UTF-8 not supported?!", e);
						}
					}
				}
			}

			html = path.startsWith("/hawthorn/html/");
			if (path.equals("/hawthorn/say"))
			{
				handleSay(params);
			}
			else if (path.equals("/hawthorn/leave"))
			{
				handleLeave(params);
			}
			else if (path.equals("/hawthorn/poll"))
			{
				handlePoll(params);
			}
			else if (path.equals("/hawthorn/waitForMessage"))
			{
				handleWaitForMessage(params);
			}
			else if (path.equals("/hawthorn/getRecent"))
			{
				handleGetRecent(params);
			}
			else if (path.equals("/hawthorn/getLog"))
			{
				handleGetLog(params);
			}
			else if (path.equals("/hawthorn/html/statistics"))
			{
				handleDisplayStatistics(params);
			}
			else
			{
				handle404(html);
			}
		}
		catch (Throwable t)
		{
			if(html)
			{
				connection.send(500,getHtmlError("Internal server error",
					"Java error: "+t.toString()),HttpServer.CONTENT_TYPE_HTML);
			}
			else
			{
				connection.send(500, "// Internal server error: " + t,
					HttpServer.CONTENT_TYPE_JAVASCRIPT);
			}
			getLogger().log(Logger.SYSTEM_LOG, Logger.Level.ERROR,
				"HTTP event error (" + Thread.currentThread().getName() + ")", t);
		}
		finally
		{
			long time = System.currentTimeMillis() - requestTime;
			getStatistics().updateTimeStatistic(
				HttpServer.STATISTICS_USER_REQUEST_TIME, (int)time);
		}
	}

	private String getHtmlError(String title, String message)
	{
		return XML.getXHTML(title, null, "<p>"+XML.esc(message)+"</p>");
	}

	private void handle404(boolean html)
	{
		if(html)
		{
			connection.send(404, getHtmlError("File not found",
				"Unknown request address:\n" + request),HttpServer.CONTENT_TYPE_HTML);
		}
		else
		{
			connection.send(404, "// Unknown request address:\n// " + request,
				HttpServer.CONTENT_TYPE_JAVASCRIPT);
		}
	}

	private void handleSay(HashMap<String, String> params)
		throws OperationException
	{
		if (!checkAuth(params, "sayError", false, false))
		{
			return;
		}
		String channel = params.get("channel");
		Channel c = getChannels().get(channel);

		String id = getID(params);

		String message = params.get("message");
		// Don't allow control characters
		if (message == null || !message.matches(Hawthorn.REGEXP_MESSAGE))
		{
			connection.send("sayEror(" + id + ",'Missing or invalid message=');");
			return;
		}

		Message m =
			new SayMessage(System.currentTimeMillis(), channel,
				connection.toString(), params.get("user"), params.get("displayname"),
				message);
		getApp().getOtherServers().sendMessage(m);
		c.message(m, false);
		connection.send("hawthorn.sayComplete(" + id + ");");
	}

	private void handleLeave(HashMap<String, String> params)
		throws OperationException
	{
		if (!checkAuth(params, "leaveError", false, false))
		{
			return;
		}
		String channel = params.get("channel");
		Channel c = getChannels().get(channel);

		Message m =
			new LeaveMessage(System.currentTimeMillis(), channel, connection
				.toString(), params.get("user"), params.get("displayname"), false);
		getApp().getOtherServers().sendMessage(m);
		c.message(m, false);

		String id = getID(params);
		connection.send("hawthorn.leaveComplete(" + id + ");");
	}

	/**
	 * Gets optional ID (default 0) from a request.
	 *
	 * @param params Param map
	 * @return ID string
	 */
	private String getID(HashMap<String, String> params)
	{
		String id = params.get("id");
		if (id == null || !id.matches("[0-9]{1,9}"))
		{
			id = "0";
		}
		return id;
	}

	private void handleGetRecent(HashMap<String, String> params)
		throws OperationException
	{
		if (!checkAuth(params, "getRecentError", false, false))
		{
			return;
		}
		Channel c = getChannels().get(params.get("channel"));

		String id = getID(params);

		String maxAge = params.get("maxage"), maxNumber = params.get("maxnumber"),
			maxNames = params.get("maxnames");
		String error = null;
		if (maxAge == null || !maxAge.matches(REGEXP_INT))
		{
			error = "Missing or invalid maxage=";
		}
		else if (maxNumber == null || !maxNumber.matches(REGEXP_INT))
		{
			error = "Missing or invalid maxnumber=";
		}
		else if (maxNames != null && !maxNames.matches(REGEXP_INT))
		{
			error = "Invalid maxnames=";
		}
		if (error != null)
		{
			connection.send("hawthorn.getRecentError(" + id + ",'"
				+ JS.escapeJS(error) + "');");
			return;
		}

		Message[] recent =
			c.getRecent(Integer.parseInt(maxAge), Integer.parseInt(maxNumber));
		Name[] names = c.getNames(maxNames == null ? Channel.ANY
			:	Integer.parseInt(maxNames));

		StringBuilder output = new StringBuilder();
		output.append("hawthorn.getRecentComplete(" + id + ",[");
		long timestamp = buildMessageArray(c, recent, output);
		output.append("],[");
		for (int i = 0; i < names.length; i++)
		{
			if (i != 0)
			{
				output.append(',');
			}
			output.append(names[i].getJSFormat());
		}
		output.append("],");
		output.append(timestamp);
		output.append(");");
		connection.send(output.toString());
	}

	private void handleWaitForMessage(HashMap<String, String> params)
		throws OperationException
	{
		if (!checkAuth(params, "waitForMessageError", false, false))
		{
			return;
		}
		Channel c = getChannels().get(params.get("channel"));

		String id = getID(params);

		String lastTimeString = params.get("lasttime");
		String error = null;
		if (!lastTimeString.matches(REGEXP_LONG))
		{
			error = "hawthorn.waitForMessageError(" + id + ",'Invalid lasttime=');";
		}
		long lastTime = Long.parseLong(lastTimeString);
		if (error != null)
		{
			connection.send("hawthorn.waitForMessageError(" + id + ",'"
				+ JS.escapeJS(error) + "');");
			return;
		}

		c.waitForMessage(connection, params.get("user"), params.get("displayname"),
			id, lastTime);
	}

	private void handlePoll(HashMap<String, String> params)
		throws OperationException
	{
		if (!checkAuth(params, "pollError", false, false))
		{
			return;
		}
		Channel c = getChannels().get(params.get("channel"));

		String id = getID(params);

		String lastTimeString = params.get("lasttime");
		String error = null;
		if (!lastTimeString.matches(REGEXP_LONG))
		{
			error = "hawthorn.pollError(" + id + ",'Invalid lasttime=');";
		}
		long lastTime = Long.parseLong(lastTimeString);
		if (error != null)
		{
			connection.send("hawthorn.pollError(" + id + ",'"
				+ JS.escapeJS(error) + "');");
			return;
		}

		long delay = c.poll(c.toString(),
			params.get("user"), params.get("displayname"));
		Message[] messages = c.getSince(lastTime, Channel.ANY);

		StringBuilder output = new StringBuilder();
		output.append("hawthorn.pollComplete(" + id + ",[");
		long timestamp = buildMessageArray(c, messages, output);
		output.append("],");
		output.append(timestamp);
		output.append(",");
		output.append(delay);
		output.append(");");
		connection.send(output.toString());
	}

	/**
	 * Creates a comma-separated list of messages from a channel, and
	 * obtains the timestamp of the last message.
	 * @param c Channel
	 * @param messages Messages to list
	 * @param output StringBuilder that receives list
	 * @return Timestamp of most recent message in list, or of just before now
	 *   if none are in list
	 */
	private long buildMessageArray(Channel c, Message[] messages,
		StringBuilder output)
	{
		long timestamp = c.getPreviousTimestamp();
		for (int i = 0; i < messages.length; i++)
		{
			if (i != 0)
			{
				output.append(',');
			}
			output.append(messages[i].getJSFormat());
			timestamp = messages[i].getTime();
		}
		return timestamp;
	}

	private void handleGetLog(HashMap<String, String> params)
		throws OperationException
	{
		if (!checkAuth(params, "getLogError", true, false))
		{
			return;
		}
		String channel = params.get("channel");

		// To retrieve log history, must use special user account
		String date = params.get("date");
		String error = null;
		if (!params.get("user").equals("_admin"))
		{
			error = "Must set user=_admin to retrieve logs";
		}
		else if (!params.get("displayname").equals("_"))
		{
			error = "Must set displayname=_ to retrieve logs";
		}
		else if (date == null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
		{
			error = "Must set date=YYYY-MM-DD";
		}
		else if (!getLogger().hasLog(channel, date))
		{
			error = "Logs not available on this server for specified date";
		}
		if (error != null)
		{
			connection.send("hawthorn.getLogError(" + getID(params) + ",'"
				+ JS.escapeJS(error) + "');");
			return;
		}

		connection.send("hawthorn.getLogComplete(" + getID(params) + ","
			+ getLogger().getLogJS(channel, date) + ");");
	}

	private void handleDisplayStatistics(HashMap<String, String> params)
	  throws OperationException
	{
		if (!checkAuth(params, null, true, true))
		{
			return;
		}

		String channel = params.get("channel");

		// To retrieve log history, must use special user account
		String error = null;
		if (!channel.equals(Logger.SYSTEM_LOG))
		{
			error = "Must request statistics via system channel";
		}
		else if (!params.get("user").equals("_admin"))
		{
			error = "Must set user=_admin to retrieve statistics";
		}
		else if (!params.get("displayname").equals("_"))
		{
			error = "Must set displayname=_ to retrieve statistics";
		}
		if (error != null)
		{
			connection.send(403, getHtmlError("Invalid statistics request",error),
				HttpServer.CONTENT_TYPE_HTML);
			return;
		}

		connection.send(200, getStatistics().getSummaryHtml(),
			HttpServer.CONTENT_TYPE_HTML);
	}

	/**
	 * Check authentication for a request.
	 *
	 * @param params HTTP parameters
	 * @param errorFunction Error function to call if params are wrong; may be
	 *        null if html is set
	 * @param allowSystemChannel If true, allows channel= to include !system
	 * @param html If true, error will be sent as HTML instead of JavaScript
	 * @return True if auth is ok, false if should return (error has already been
	 *         sent)
	 * @throws OperationException Problem with hashing function
	 */
	private boolean checkAuth(HashMap<String, String> params,
		String errorFunction, boolean allowSystemChannel, boolean html) throws OperationException
	{
		String channel = params.get("channel"), user = params.get("user"), displayname =
			params.get("displayname"), key = params.get("key"), keytime =
			params.get("keytime");

		String error = null;
		if (channel == null
			|| (!channel.matches(Hawthorn.REGEXP_USERCHANNEL) && !(allowSystemChannel && channel
				.equals(Logger.SYSTEM_LOG))))
		{
			error = "Missing or invalid channel=";
		}
		else if (user == null || !user.matches(Hawthorn.REGEXP_USERCHANNEL))
		{
			error = "Missing or invalid user=";
		}
		else if (displayname == null
			|| !displayname.matches(Hawthorn.REGEXP_DISPLAYNAME))
		{
			// Displayname can't contain control characters or "
			error = "Missing or invalid displayname=";
		}
		else if (key == null)
		{
			error = "Missing key=";
		}
		else if (keytime == null || !keytime.matches(REGEXP_LONG))
		{
			error = "Missing or invalid keytime=";
		}
		else if (Long.parseLong(keytime) < System.currentTimeMillis())
		{
			error = "Expired key";
		}
		else if (!key.equals(getApp().getValidKey(channel, user, displayname,
			Long.parseLong(keytime))))
		{
			error = "Invalid key";
		}

		if (error != null)
		{
			if(html)
			{
				connection.send(403,getHtmlError("Authorisation failed",
					error),HttpServer.CONTENT_TYPE_HTML);
			}
			else
			{
				connection.send("hawthorn." + errorFunction + "(" + getID(params) + ",'"
					+ JS.escapeJS(error) + "');");
			}
			return false;
		}

		return true;
	}


}
