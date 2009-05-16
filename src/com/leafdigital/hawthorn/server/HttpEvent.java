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

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

import com.leafdigital.hawthorn.server.Logger.Level;
import com.leafdigital.hawthorn.util.*;
import com.leafdigital.hawthorn.util.Auth.Permission;

/** Event generated by HTTP request. Handles the user's request. */
public class HttpEvent extends Event
{
	/** Request type: say */
	static final String SAY = "say";
	/** Request type: say */
	static final String BAN = "ban";
	/** Request type: leave */
	static final String LEAVE = "leave";
	/** Request type: poll */
	static final String POLL = "poll";
	/** Request type: wait */
	static final String WAIT = "wait";
	/** Request type: recent */
	static final String RECENT = "recent";
	/** Request type: log */
	static final String LOG = "log";
	/** Request type: statistics */
	static final String STATISTICS = "statistics";
	/** Request type: favicon */
	static final String FAVICON = "favicon";

	/** Regular expression matching positive longs */
	static final String REGEXP_LONG = "[0-9]{1,18}";

	/** Regular expression matching positive ints */
	static final String REGEXP_INT = "[0-9]{1,9}";

	/** Favicon data */
	private static byte[] favIcon=null;

	private String request;

	private HttpServer.Connection connection;

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
	}

	@Override
	public void handle() throws OperationException
	{
		String requestType = null;
		boolean html=false;
		try
		{
			String path;
			HashMap<String, String> params = new HashMap<String, String>();

			int question = request.indexOf('?');
			if(question == -1)
			{
				path = request;
			}
			else
			{
				path = request.substring(0, question);
				String remainder = request.substring(question + 1);
				while(remainder.length() > 0)
				{
					int and = remainder.indexOf('&');
					String paramPair;
					if(and == -1)
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
					if(equals == -1)
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
						catch(UnsupportedEncodingException e)
						{
							throw new Error("UTF-8 not supported?!", e);
						}
					}
				}
			}

			html = path.startsWith("/hawthorn/html/");
			if(path.equals("/hawthorn/say"))
			{
				requestType = SAY;
				handleSay(params);
			}
			else if(path.equals("/hawthorn/ban"))
			{
				requestType = BAN;
				handleBan(params);
			}
			else if(path.equals("/hawthorn/leave"))
			{
				requestType = LEAVE;
				handleLeave(params);
			}
			else if(path.equals("/hawthorn/poll"))
			{
				requestType = POLL;
				handlePoll(params);
			}
			else if(path.equals("/hawthorn/wait"))
			{
				requestType = WAIT;
				handleWait(params);
			}
			else if(path.equals("/hawthorn/recent"))
			{
				requestType = RECENT;
				handleRecent(params);
			}
			else if(path.equals("/hawthorn/log"))
			{
				requestType = LOG;
				handleLog(params);
			}
			else if(path.equals("/hawthorn/html/statistics"))
			{
				requestType = STATISTICS;
				handleDisplayStatistics(params);
			}
			else if(path.equals("/favicon.ico"))
			{
				requestType = FAVICON;
				handleDisplayFavicon();
			}
			else
			{
				handle404(html);
			}
		}
		catch(Throwable t)
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
			long time = System.currentTimeMillis() - connection.getStartTime();
			getStatistics().updateTimeStatistic(
				HttpServer.STATISTIC_USER_REQUEST_TIME, (int)time);
			if(requestType!=null && getConfig().isDetailedStats())
			{
				getStatistics().updateTimeStatistic(
					HttpServer.STATISTIC_SPECIFIC_REQUEST + requestType, (int)time);
			}
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
		String errorFunction = "sayError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, false, false);
		if(permissionSet == null)
		{
			return;
		}
		Channel c = checkChannel(params, errorFunction);
		if(c==null)
		{
			return;
		}

		String id = getID(params);

		String message = params.get("message");
		String unique = params.get("unique");

		String error = null;
		if(message == null || !message.matches(Hawthorn.REGEXP_MESSAGE))
		{
			error = "Missing or invalid message=";
		}
		else if(!permissionSet.contains(Permission.WRITE))
		{
			error = "Must have write permission to [say]";
		}
		else if(unique == null || !unique.matches(REGEXP_LONG))
		{
			error = "Missing unique=";
		}
		if(error != null)
		{
			connection.send("hawthorn.sayError(" + id + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		String user = params.get("user");
		Message m = new SayMessage(System.currentTimeMillis(), c.getName(),
			connection.toString(), user, getApp().getMaskedUser(user),
			params.get("displayname"), params.get("extra"), unique, message);
		getApp().getOtherServers().sendMessage(m);
		c.message(m, false);
		connection.send("hawthorn.sayComplete(" + id + ");");
	}

	private void handleBan(HashMap<String, String> params)
		throws OperationException
	{
		String errorFunction = "banError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, false, false);
		if(permissionSet == null)
		{
			return;
		}
		Channel c = checkChannel(params, errorFunction);
		if(c==null)
		{
			return;
		}

		String id = getID(params);

		String
			ban = params.get("ban"),
			banDisplayName = params.get("bandisplayname"),
			banExtra = params.get("banextra"),
			unique = params.get("unique"),
			untilText = params.get("until");

		String error = null;
		if(ban == null || !ban.matches(Hawthorn.REGEXP_USERCHANNEL))
		{
			error = "Missing or invalid ban=";
		}
		else if(banDisplayName == null ||
			!banDisplayName.matches(Hawthorn.REGEXP_DISPLAYNAME))
		{
			error = "Missing or invalid bandisplayname=";
		}
		else if(banExtra == null ||
			!banExtra.matches(Hawthorn.REGEXP_EXTRA))
		{
			error = "Missing or invalid bandisplayname=";
		}
		else if(untilText==null || !untilText.matches(REGEXP_LONG))
		{
			error = "Missing or invalid until=";
		}
		else if(!permissionSet.contains(Permission.MODERATE))
		{
			error = "Must have moderate permission to [ban]";
		}
		if(error != null)
		{
			connection.send("hawthorn.banError(" + id + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		String user = params.get("user");
		Message m = new BanMessage(System.currentTimeMillis(), c.getName(),
			connection.toString(), user, getApp().getMaskedUser(user),
			params.get("displayname"), params.get("extra"), unique, ban,
			getApp().getMaskedUser(ban), banDisplayName, banExtra,
			Long.parseLong(untilText));
		getApp().getOtherServers().sendMessage(m);
		c.message(m, false);
		connection.send("hawthorn.banComplete(" + id + ");");
	}

	private void handleLeave(HashMap<String, String> params)
		throws OperationException
	{
		String errorFunction = "leaveError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, false, false);
		if(permissionSet == null)
		{
			return;
		}
		Channel c = checkChannel(params, errorFunction);
		if(c==null)
		{
			return;
		}

		String id = getID(params);

		String error = null;
		if(!permissionSet.contains(Permission.READ))
		{
			error = "Must have read permission to [leave]";
		}
		if(error != null)
		{
			connection.send("hawthorn.recentError(" + id + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		String user = params.get("user");
		Message m = new LeaveMessage(System.currentTimeMillis(), c.getName(),
				connection.toString(), user, getApp().getMaskedUser(user),
				params.get("displayname"), params.get("extra"), false);
		getApp().getOtherServers().sendMessage(m);
		c.message(m, false);

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
		if(id == null || !id.matches("[0-9]{1,9}"))
		{
			id = "0";
		}
		return id;
	}

	private void handleRecent(HashMap<String, String> params)
		throws OperationException
	{
		String errorFunction = "recentError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, false, false);
		if(permissionSet == null)
		{
			return;
		}
		Channel c = checkChannel(params, errorFunction);
		if(c==null)
		{
			return;
		}

		String id = getID(params);

		String maxAge = params.get("maxage"), maxNumber = params.get("maxnumber"),
			maxNames = params.get("maxnames"), filter = params.get("filter");
		String error = null;
		if(maxAge == null || !maxAge.matches(REGEXP_INT))
		{
			error = "Missing or invalid maxage=";
		}
		else if(Integer.parseInt(maxAge) > getConfig().getHistoryTime())
		{
			error = "maxage= is set longer than server history time";
		}
		else if(!permissionSet.contains(Permission.READ))
		{
			error = "Must have read permission to [recent]";
		}
		else if(maxNumber == null || !maxNumber.matches(REGEXP_INT))
		{
			error = "Missing or invalid maxnumber=";
		}
		else if(maxNames != null && !maxNames.matches(REGEXP_INT))
		{
			error = "Invalid maxnames=";
		}
		else if(filter != null && !filter.equals("say"))
		{
			error = "Invalid filter=";
		}
		if(error != null)
		{
			connection.send("hawthorn.recentError(" + id + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		Message[] recent =
			c.recent(Integer.parseInt(maxAge), Integer.parseInt(maxNumber), filter!=null);
		Name[] names = c.getNames(maxNames == null ? Channel.ANY
			:	Integer.parseInt(maxNames));

		StringBuilder output = new StringBuilder();
		output.append("hawthorn.recentComplete(" + id + ",[");
		boolean trusted = permissionSet.contains(Permission.MODERATE);
		long timestamp = buildMessageArray(c, recent, trusted, output);
		output.append("],[");
		for(int i = 0; i < names.length; i++)
		{
			if(i != 0)
			{
				output.append(',');
			}
			output.append(names[i].getJSFormat(trusted));
		}
		output.append("],");
		output.append(timestamp);
		output.append(");");
		connection.send(output.toString());
	}

	private void handleWait(HashMap<String, String> params)
		throws OperationException
	{
		String errorFunction = "waitError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, false, false);
		if(permissionSet == null)
		{
			return;
		}
		Channel c = checkChannel(params, errorFunction);
		if(c==null)
		{
			return;
		}

		String id = getID(params);

		String lastTimeString = params.get("lasttime");
		String error = null;
		if(!lastTimeString.matches(REGEXP_LONG))
		{
			error = "Invalid lasttime=";
		}
		else if(!permissionSet.contains(Permission.READ))
		{
			error = "Must have read permission to [wait]";
		}
		if(error != null)
		{
			connection.send("hawthorn.waitError(" + id + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		long lastTime = Long.parseLong(lastTimeString);
		c.wait(connection, params.get("user"), params.get("displayname"),
			params.get("extra"), id, lastTime,
			permissionSet.contains(Permission.MODERATE));
	}

	private void handlePoll(HashMap<String, String> params)
		throws OperationException
	{
		String errorFunction = "pollError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, false, false);
		if(permissionSet == null)
		{
			return;
		}
		Channel c = checkChannel(params, errorFunction);
		if(c==null)
		{
			return;
		}

		String id = getID(params);

		String lastTimeString = params.get("lasttime");
		String error = null;
		if(!lastTimeString.matches(REGEXP_LONG))
		{
			error = "Invalid lasttime=";
		}
		else if(!permissionSet.contains(Permission.READ))
		{
			error = "Must have read permission to [poll]";
		}
		if(error != null)
		{
			connection.send("hawthorn.pollError(" + id + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		long lastTime = Long.parseLong(lastTimeString);
		long delay = c.poll(connection.toString(),
			params.get("user"), params.get("displayname"), params.get("extra"));
		Message[] messages = c.getSince(lastTime, Channel.ANY, false);

		StringBuilder output = new StringBuilder();
		output.append("hawthorn.pollComplete(" + id + ",[");
		long timestamp = buildMessageArray(c, messages,
			permissionSet.contains(Permission.MODERATE), output);
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
	 * @param trusted True if user is trusted to see user names not just
	 *   display names
	 * @param output StringBuilder that receives list
	 * @return Timestamp of most recent message in list, or of just before now
	 *   if none are in list
	 */
	private long buildMessageArray(Channel c, Message[] messages,
		boolean trusted, StringBuilder output)
	{
		long timestamp = c.getPreviousTimestamp();
		for(int i = 0; i < messages.length; i++)
		{
			if(i != 0)
			{
				output.append(',');
			}
			output.append(messages[i].getJSFormat(trusted));
			timestamp = messages[i].getTime();
		}
		return timestamp;
	}

	private void handleLog(HashMap<String, String> params)
		throws OperationException
	{
		String errorFunction = "logError";
		EnumSet<Permission> permissionSet =
			checkAuth(params, errorFunction, true, false);
		if(permissionSet == null)
		{
			return;
		}
		String channel = params.get("channel");

		// To retrieve log history, must use special user account
		String date = params.get("date");
		String error = null;
		if(!permissionSet.contains(Permission.ADMIN))
		{
			error = "Must have admin permission to [log]";
		}
		else if(date == null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
		{
			error = "Must set date=YYYY-MM-DD";
		}
		else if(!getLogger().hasLog(channel, date))
		{
			error = "Logs not available on this server for specified date";
		}
		if(error != null)
		{
			connection.send("hawthorn.logError(" + getID(params) + ",'"
				+ JS.esc(error) + "');");
			return;
		}

		getLogger().log(Logger.SYSTEM_LOG, Level.NORMAL,
			"AUDIT LOG " + params.get("user") + " (" + connection.toString()
			+ ") obtained log for " + channel + " on " + date);
		connection.send("hawthorn.logComplete(" + getID(params) + ","
			+ getLogger().getLogJS(channel, date) + ");");
	}

	private void handleDisplayStatistics(HashMap<String, String> params)
	  throws OperationException
	{
		EnumSet<Permission> permissionSet = checkAuth(params, null, true, true);
		if(permissionSet == null)
		{
			return;
		}

		String channel = params.get("channel");

		// To retrieve log history, must use special user account
		String error = null;
		if(!channel.equals(Logger.SYSTEM_LOG))
		{
			error = "Must request statistics via system channel";
		}
		else if(!permissionSet.contains(Permission.ADMIN))
		{
			error = "Must have admin permission to [statistics]";
		}
		if(error != null)
		{
			connection.send(403, getHtmlError("Invalid statistics request",error),
				HttpServer.CONTENT_TYPE_HTML);
			return;
		}

		if(params.get("gc")!=null)
		{
			long before = System.currentTimeMillis();
			System.gc();
			getLogger().log(Logger.SYSTEM_LOG, Level.NORMAL,
				"AUDIT STATISTICS " + params.get("user") + " (" + connection.toString()
				+ ") ran garbage collection ("
				+ (System.currentTimeMillis() - before) + "ms)");
			connection.send(302, getConfig().getThisServer().getURL()
				+ request.replace("&gc=y",""), HttpServer.CONTENT_TYPE_REDIRECT);
			return;
		}

		getLogger().log(Logger.SYSTEM_LOG, Level.NORMAL,
			"AUDIT STATISTICS " + params.get("user") + " (" + connection.toString()
			+ ") viewed statistics page");
		connection.send(200, getStatistics().getSummaryHtml(request),
			HttpServer.CONTENT_TYPE_HTML);
	}

	private void handleDisplayFavicon() throws OperationException
	{
		// Load favicon
		if(favIcon == null)
		{
			try
			{
				InputStream input = HttpEvent.class.getResourceAsStream(
					"hawthorn.favicon.ico");
				byte[] buffer = new byte[65536];
				int pos = 0;
				while(pos < buffer.length)
				{
					int read = input.read(buffer, pos, buffer.length - pos);
					if(read == -1)
					{
						break;
					}
					pos += read;
				}
				input.close();

				favIcon = new byte[pos];
				System.arraycopy(buffer, 0, favIcon, 0, favIcon.length);
			}
			catch(IOException e)
			{
				throw new OperationException(ErrorCode.OPERATION_FAVICONREAD,
					"Failed to load favicon data", e);
			}
		}

		// Send data
		connection.send(200, favIcon, HttpServer.CONTENT_TYPE_ICON, null, true);
	}

	/**
	 * Check authentication for a request.
	 *
	 * @param params HTTP parameters
	 * @param errorFunction Error function to call if params are wrong; may be
	 *   null if html is set
	 * @param allowSystemChannel If true, allows channel= to include !system
	 * @param html If true, error will be sent as HTML instead of JavaScript
	 * @return Permission set if auth is ok, false if should return
	 *   (error has already been sent)
	 * @throws OperationException Problem with hashing function
	 */
	private EnumSet<Permission> checkAuth(HashMap<String, String> params,
		String errorFunction, boolean allowSystemChannel, boolean html)
		throws OperationException
	{
		String channel = params.get("channel"), user = params.get("user"),
			displayname =	params.get("displayname"), extra = params.get("extra"),
			key = params.get("key"), keytime = params.get("keytime");

		// Check permissions if supplied
		String permissions = params.get("permissions");
		EnumSet<Permission> permissionSet = null;
		if(permissions != null)
		{
			try
			{
				permissionSet = Auth.getPermissionSet(permissions);
			}
			catch(IllegalArgumentException e)
			{
				sendPermissionError(params, errorFunction, html, "Invalid permission=");
				return null;
			}
		}

		String error = null;
		if(channel == null
			|| (!channel.matches(Hawthorn.REGEXP_USERCHANNEL) && !(allowSystemChannel && channel
				.equals(Logger.SYSTEM_LOG))))
		{
			error = "Missing or invalid channel=";
		}
		else if(user == null || !user.matches(Hawthorn.REGEXP_USERCHANNEL))
		{
			error = "Missing or invalid user=";
		}
		else if(displayname == null
			|| !displayname.matches(Hawthorn.REGEXP_DISPLAYNAME))
		{
			// Displayname can't contain control characters or "
			error = "Missing or invalid displayname=";
		}
		else if(extra == null || !extra.matches(Hawthorn.REGEXP_EXTRA))
		{
			error = "Missing or invalid extra=";
		}
		else if(permissions == null)
		{
			error = "Missing permissions=";
		}
		else if(key == null)
		{
			error = "Missing key=";
		}
		else if(keytime == null || !keytime.matches(REGEXP_LONG))
		{
			error = "Missing or invalid keytime=";
		}
		else if(Long.parseLong(keytime) < System.currentTimeMillis())
		{
			error = "Expired key";
		}
		else if(!key.equals(getApp().getValidKey(channel, user, displayname, extra,
			permissionSet, Long.parseLong(keytime))))
		{
			error = "Invalid key";
		}

		if(error != null)
		{
			sendPermissionError(params, errorFunction, html, error);
			return null;
		}

		return permissionSet;
	}

	/**
	 * Gets channel and checks that user isn't banned.
	 * @param params HTTP parameters
	 * @param errorFunction Name of error function
	 * @return Channel or null if user is banned
	 */
	private Channel checkChannel(HashMap<String, String> params,
		String errorFunction)
	{
		Channel c = getChannels().get(params.get("channel"));
		if(c.isBanned(params.get("user")))
		{
			sendPermissionError(params, errorFunction, false, "You are banned");
			return null;
		}
		else
		{
			return c;
		}
	}

	/**
	 * Sends an error when permission checks fail.
	 * @param params Parameters
	 * @param errorFunction Name of error JS function
	 * @param html True to send error in HTML instead of JS
	 * @param error Error text
	 */
	private void sendPermissionError(HashMap<String, String> params,
		String errorFunction, boolean html, String error)
	{
		if(html)
		{
			connection.send(403,getHtmlError("Authorisation failed",
				error),HttpServer.CONTENT_TYPE_HTML);
		}
		else
		{
			connection.send("hawthorn." + errorFunction + "(" + getID(params) + ",'"
				+ JS.esc(error) + "');");
		}
	}


}
