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
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.leafdigital.hawthorn.util.*;

/** Configuration file reader. */
public class Configuration
{
	/** Test keys expire after 12 hours */
	private final static long TEST_KEY_EXPIRY = 12*60*60*1000;

	private String magicNumber;
	private boolean logChat = true;
	private long historyTime = 15*60*1000;
	private int eventThreads = 4;
	private int minPollTime = 2000, maxPollTime = 15000, pollScaleTime = 60000;
	private ServerInfo[] otherServers;
	private ServerInfo thisServer;
	private Logger.Level minLogLevel = Logger.Level.NORMAL;
	private LinkedList<TestKey> testKeys = new LinkedList<TestKey>();
	private static long testKeyTime = System.currentTimeMillis() + TEST_KEY_EXPIRY;
	private boolean detailedStats = false;
	private String ipHeader = null;

	private Logger logger;

	/** Contains details about a single server */
	public static class ServerInfo
	{
		private InetAddress address;
		private int port;

		/**
		 * Interprets info from config file.
		 *
		 * @param details XML element containing server details
		 * @throws StartupException
		 */
		private ServerInfo(Element details) throws StartupException
		{
			try
			{
				address = InetAddress.getByName(getText(details));
			}
			catch(UnknownHostException e)
			{
				throw new StartupException(ErrorCode.STARTUP_INVALIDADDRESS,
					"The Internet address " + getText(details) + " is not valid.");
			}

			if(details.hasAttribute("port"))
			{
				try
				{
					port = Integer.parseInt(details.getAttribute("port"));
					if(port <= 0 || port > 32767)
					{
						throw new NumberFormatException();
					}
				}
				catch(NumberFormatException e)
				{
					throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
						"The port= value is not a valid port number.");
				}
			}
			else
			{
				port = 13370;
			}
		}

		/** @return Address */
		public InetAddress getAddress()
		{
			return address;
		}

		/** @return Port */
		public int getPort()
		{
			return port;
		}

		@Override
		public String toString()
		{
			return address.getHostAddress() + " port=" + port;
		}
	}

	/**
	 * Obtains text from inside an XML element.
	 *
	 * @param container Containing element
	 * @return Text string (will have whitespace trimmed)
	 * @throws StartupException If there is anything other than text in there
	 */
	private static String getText(Element container) throws StartupException
	{
		try
		{
			return XML.getText(container);
		}
		catch(IOException e)
		{
			throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT, e.getMessage());
		}
	}

	/**
	 * Loads the configuration.
	 *
	 * @param configFile File to load
	 * @throws StartupException If there is any error with the config file
	 */
	public Configuration(File configFile) throws StartupException
	{
		File logFolder = null;
		int logDays = 7;

		try
		{
			// Parse file
			Document xml = XML.getDocumentBuilder().parse(configFile);
			if(!xml.getDocumentElement().getTagName().equals("hawthorn"))
			{
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Configuration file root element must be <hawthorn>.");
			}

			// Go through each child element
			NodeList children = xml.getDocumentElement().getChildNodes();
			for(int i = 0; i < children.getLength(); i++)
			{
				if(!(children.item(i) instanceof Element))
				{
					continue;
				}
				Element child = (Element)children.item(i);
				if(child.getTagName().equals("magicnumber"))
				{
					magicNumber = getText(child);
				}
				else if(child.getTagName().equals("logfolder"))
				{
					logFolder = new File(getText(child));
					if(!logFolder.canWrite())
					{
						throw new StartupException(ErrorCode.STARTUP_LOGNOTWRITEABLE,
							"The specified log folder " + logFolder + " does not exist, or "
								+ "else the system cannot write to it. Please ensure it is a "
								+ "writable folder.");
					}
				}
				else if(child.getTagName().equals("logdays"))
				{
					try
					{
						logDays = Integer.parseInt(getText(child));
						if(logDays < 0)
						{
							throw new NumberFormatException();
						}
					}
					catch(NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <logdays> value is not a valid number.");
					}
				}
				else if(child.getTagName().equals("loglevel"))
				{
					try
					{
						minLogLevel = Logger.Level.valueOf(getText(child));
					}
					catch(IllegalArgumentException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <loglevel> value is not a valid log level. Valid options "
								+ "include DETAIL, NORMAL, and ERROR.");
					}
				}
				else if(child.getTagName().equals("logchat"))
				{
					if(getText(child).equals("y"))
					{
						logChat = true;
					}
					else if(getText(child).equals("n"))
					{
						logChat = false;
					}
					else
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <logchat> value is not valid. "
								+ "Please use y or n.");
					}
				}
				else if(child.getTagName().equals("detailedstats"))
				{
					if(getText(child).equals("y"))
					{
						detailedStats = true;
					}
					else if(getText(child).equals("n"))
					{
						detailedStats = false;
					}
					else
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <detailedstats> value is not valid. "
								+ "Please use y or n.");
					}
				}
				else if(child.getTagName().equals("historytime"))
				{
					try
					{
						historyTime = Long.parseLong(getText(child));
						if(historyTime < 0)
						{
							throw new NumberFormatException();
						}
					}
					catch(NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <historytime> value is not a valid number.");
					}
				}
				else if(child.getTagName().equals("minpoll"))
				{
					try
					{
						minPollTime = Integer.parseInt(getText(child));
						if(minPollTime <= 0)
						{
							throw new NumberFormatException();
						}
					}
					catch(NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <minpoll> value is not a valid number.");
					}
				}
				else if(child.getTagName().equals("maxpoll"))
				{
					try
					{
						maxPollTime = Integer.parseInt(getText(child));
						if(maxPollTime <= 0)
						{
							throw new NumberFormatException();
						}
					}
					catch(NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <maxpoll> value is not a valid number.");
					}
				}
				else if(child.getTagName().equals("pollscale"))
				{
					try
					{
						pollScaleTime = Integer.parseInt(getText(child));
						if(pollScaleTime <= 0)
						{
							throw new NumberFormatException();
						}
					}
					catch(NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <pollscale> value is not a valid number.");
					}
				}
				else if(child.getTagName().equals("ipheader"))
				{
					ipHeader = getText(child);
				}
				else if(child.getTagName().equals("servers"))
				{
					LinkedList<ServerInfo> otherServersList =
						new LinkedList<ServerInfo>();
					NodeList servers = child.getChildNodes();
					for(int j = 0; j < servers.getLength(); j++)
					{
						if(!(servers.item(j) instanceof Element))
						{
							continue;
						}
						Element server = (Element)servers.item(j);
						if(!server.getTagName().equals("server"))
						{
							throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
								"The <servers> tag may only contain <server> tags.");
						}
						if("y".equals(server.getAttribute("this")))
						{
							thisServer = new ServerInfo(server);
						}
						else
						{
							otherServersList.add(new ServerInfo(server));
						}
					}
					otherServers =
						otherServersList.toArray(new ServerInfo[otherServersList.size()]);
				}
				else if(child.getTagName().equals("eventthreads"))
				{
					try
					{
						eventThreads = Integer.parseInt(getText(child));
						if(eventThreads < 1 || eventThreads > 100)
						{
							throw new NumberFormatException();
						}
					}
					catch(NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <eventthreads> value is not a valid thread count number.");
					}
				}
				else if(child.getTagName().equals("testkey"))
				{
					String
						channel = child.getAttribute("channel"),
						user = child.getAttribute("user"),
						displayName =	child.getAttribute("displayname"),
						extra = child.getAttribute("extra"),
						permissions = child.getAttribute("permissions");
					if(extra == null)
					{
						extra = "";
					}
					if(channel == null
						|| (!channel.matches(Hawthorn.REGEXP_USERCHANNEL) &&
							!channel.equals(Logger.SYSTEM_LOG)))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires channel=, with no special characters");
					}
					if(user == null || !user.matches(Hawthorn.REGEXP_USERCHANNEL))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires user=, with no special characters");
					}
					if(displayName == null
						|| !displayName.matches(Hawthorn.REGEXP_DISPLAYNAME))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires displayname=, with no double quotes");
					}
					if(!extra.matches(Hawthorn.REGEXP_EXTRA))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires displayname=, with no double quotes");
					}
					testKeys.add(new TestKey(channel, user, displayName, extra,
						permissions));
				}
				else
				{
					throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
						"The tag <" + child.getTagName() + "> is not recognised.");
				}
			}

			if(logFolder == null)
			{
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Missing required <logfolder> configuration element. Specify a "
						+ "folder where logs should be stored. Example:\n\n"
						+ "<logfolder>/var/logs/jschat</logfolder>");
			}
			if(magicNumber == null)
			{
				String sha1;
				try
				{
					sha1 = Auth.hash(Math.random() + "");
				}
				catch(NoSuchAlgorithmException e)
				{
					throw new Error("Missing SHA-1 support");
				}
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Missing required <magicnumber> configuration element. Specify a "
						+ "suitable random string that will be kept secure. Here's one "
						+ "generated for you:\n\n<magicnumber>" + sha1 + "</magicnumber>");
			}
			if(thisServer == null)
			{
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Missing definition for current server. Here is a <servers> section "
						+ "that may be suitable for a single-server installation on this "
						+ "computer:\n\n<servers>\n  <server this='y'>"
						+ InetAddress.getLocalHost().getHostAddress()
						+ "</server>\n</servers>");
			}

			logger =
				new Logger(logFolder, minLogLevel, logDays, thisServer.getAddress(),
					thisServer.getPort());
			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
				"Hawthorn system startup");

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL, "Logs: "
				+ logFolder + " (level "+ minLogLevel + "; deleted after "
				+ (logDays == 0 ? "never" : logDays + " days; ")
				+ (logChat ? "chat logged" : "chat not logged") + ")");

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
				"Poll delay scale: " + minPollTime + "-" + maxPollTime + "ms, over "
				+ pollScaleTime + "ms");

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
				"History retained for: " + (historyTime/60000L) + " minutes");

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
				"Event threads: " + eventThreads);

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
				"Detailed stats: " + (detailedStats ? "tracked" : "not tracked"));

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
				"IP header: " + (ipHeader==null ? "(none)" : ipHeader));

			logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL, "This server: "
				+ thisServer);
			for(int i = 0; i < otherServers.length; i++)
			{
				logger.log(Logger.SYSTEM_LOG, Logger.Level.NORMAL, "Remote server: "
					+ otherServers[i]);
			}
		}
		catch(ParserConfigurationException e)
		{
			throw new StartupException(ErrorCode.STARTUP_XMLSYSTEM,
				"The XML system appears to be incorrectly configured.");
		}
		catch(SAXException e)
		{
			throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
				"The configuration file is not valid XML.", e);
		}
		catch(IOException e)
		{
			throw new StartupException(ErrorCode.STARTUP_CONFIGREAD,
				"Unable to read the configuration file.", e);
		}
	}

	/**
	 * Stores details for a test key we're going to generate.
	 */
	public static class TestKey
	{
		private String channel, user, displayName, extra, permissions;

		/**
		 * @param channel Channel name
		 * @param user User name
		 * @param displayName Display name
		 * @param extra Extra per-user data
		 * @param permissions Permissions
		 */
		TestKey(String channel, String user, String displayName, String extra,
			String permissions)
		{
			this.channel = channel;
			this.user = user;
			this.displayName = displayName;
			this.extra = extra;
			this.permissions = (permissions == null || permissions.equals(""))
				? "rw" : permissions;
		}

		/**
		 * Displays the test key on standard error.
		 *
		 * @param app Main application object
		 * @throws StartupException
		 */
		void show(Hawthorn app) throws StartupException
		{
			try
			{
				System.out.println("     Channel: " + channel);
				System.out.println("        User: " + user);
				System.out.println("Display name: " + displayName);
				System.out.println("       Extra: " + extra);
				System.out.println(" Permissions: " + permissions);
				System.out.println("        Time: " + testKeyTime);
				System.out.println();
				String key =
					app.getValidKey(channel, user, displayName, extra,
						Auth.getPermissionSet(permissions), testKeyTime);
				System.out.println("         Key: " + key);
				System.out.println();
			}
			catch(Exception e)
			{
				throw new StartupException(ErrorCode.STARTUP_TESTKEY,
					"Error generating test key", e);
			}
		}
	}

	/** @return Information about this server address */
	public ServerInfo getThisServer()
	{
		return thisServer;
	}

	/** @return Logger */
	public Logger getLogger()
	{
		return logger;
	}

	/** @return Number of event threads to create */
	public int getEventThreads()
	{
		return eventThreads;
	}

	/** @return Magic number string */
	public String getMagicNumber()
	{
		return magicNumber;
	}

	/** @return History time to keep messages for(ms) */
	public long getHistoryTime()
	{
		return historyTime;
	}

	/** @return True if chat messages should be logged at this server */
	public boolean logChat()
	{
		return logChat;
	}

	/** @return Minimum suggested time for clients to poll (ms) */
	public int getMinPollTime()
	{
		return minPollTime;
	}

	/** @return Maximum suggested time for clients to poll (ms) */
	public int getMaxPollTime()
	{
		return maxPollTime;
	}

	/** @return Time since last message at which the suggested poll time
	 *   will reach max poll time (immediately after a message, it is at
	 *   the minimum time) */
	public int getPollScaleTime()
	{
		return pollScaleTime;
	}

	/** @return True if stats should be tracked for specific event types */
	public boolean isDetailedStats()
	{
		return detailedStats;
	}

	/**
	 * @return Name (not case-sensitive) of HTTP header that is trusted to
	 *   provide the source IP address, or null if none; applies only to
	 *   user connections, as server connections must be direct */
	public String getIpHeader()
	{
		return ipHeader;
	}

	/**
	 * Checks whether a given address belongs to one of the other servers.
	 *
	 * @param possible Address that might be another server
	 * @return True if it is
	 */
	public boolean isOtherServer(InetAddress possible)
	{
		for(ServerInfo compare : otherServers)
		{
			if(compare.getAddress().getHostAddress().equals(
				possible.getHostAddress()))
			{
				return true;
			}
		}
		return false;
	}

	/** @return List of other servers */
	public ServerInfo[] getOtherServers()
	{
		ServerInfo[] result = new ServerInfo[otherServers.length];
		System.arraycopy(otherServers, 0, result, 0, otherServers.length);
		return result;
	}

	/** @return Keys to display for test purposes */
	public Collection<TestKey> getTestKeys()
	{
		return testKeys;
	}

}
