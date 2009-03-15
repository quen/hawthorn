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
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

/** Configuration file reader. */
public class Configuration
{
	private String magicNumber;
	private boolean logChat = true;
	private int historyHours = 4;
	private int eventThreads = 4;
	private ServerInfo[] otherServers;
	private ServerInfo thisServer;
	private Logger.Level minLogLevel = Logger.Level.NORMAL;
	private LinkedList<TestKey> testKeys = new LinkedList<TestKey>();
	private static long testKeyTime = System.currentTimeMillis();

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
			catch (UnknownHostException e)
			{
				throw new StartupException(ErrorCode.STARTUP_INVALIDADDRESS,
					"The Internet address " + getText(details) + " is not valid.");
			}

			if (details.hasAttribute("port"))
			{
				try
				{
					port = Integer.parseInt(details.getAttribute("port"));
					if (port <= 0 || port > 32767)
					{
						throw new NumberFormatException();
					}
				}
				catch (NumberFormatException e)
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
		StringBuilder result = new StringBuilder();
		NodeList children = container.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			if (!(children.item(i) instanceof Text))
			{
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT, "Element "
					+ container.getTagName() + " must not include XML tags.");
			}
			result.append(((Text)children.item(i)).getData());
		}
		return result.toString().trim();
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
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;
			db = dbf.newDocumentBuilder();
			Document xml = db.parse(configFile);
			if (!xml.getDocumentElement().getTagName().equals("jschat"))
			{
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Configuration file root element must be <jschat>.");
			}

			// Go through each child element
			NodeList children = xml.getDocumentElement().getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
			{
				if (!(children.item(i) instanceof Element))
				{
					continue;
				}
				Element child = (Element)children.item(i);
				if (child.getTagName().equals("magicnumber"))
				{
					magicNumber = getText(child);
				}
				else if (child.getTagName().equals("logfolder"))
				{
					logFolder = new File(getText(child));
					if (!logFolder.canWrite())
					{
						throw new StartupException(ErrorCode.STARTUP_LOGNOTWRITEABLE,
							"The specified log folder " + logFolder + " does not exist, or "
								+ "else the system cannot write to it. Please ensure it is a "
								+ "writable folder.");
					}
				}
				else if (child.getTagName().equals("logdays"))
				{
					try
					{
						logDays = Integer.parseInt(getText(child));
						if (logDays < 0)
						{
							throw new NumberFormatException();
						}
					}
					catch (NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <logdays> value is not a valid number.");
					}
				}
				else if (child.getTagName().equals("loglevel"))
				{
					try
					{
						minLogLevel = Logger.Level.valueOf(getText(child));
					}
					catch (IllegalArgumentException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <loglevel> value is not a valid log level. Valid options "
								+ "include DETAIL, NORMAL, and ERROR.");
					}
				}
				else if (child.getTagName().equals("logchat"))
				{
					if (getText(child).equals("y"))
					{
						logChat = true;
					}
					else if (getText(child).equals("n"))
					{
						logChat = false;
					}
					else
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <logchat> value is not a valid log chat setting. "
								+ "Please use y or n.");
					}
				}
				else if (child.getTagName().equals("historyhours"))
				{
					try
					{
						historyHours = Integer.parseInt(getText(child));
						if (historyHours < 0)
						{
							throw new NumberFormatException();
						}
					}
					catch (NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <historyhours> value is not a valid number.");
					}
				}
				else if (child.getTagName().equals("servers"))
				{
					LinkedList<ServerInfo> otherServersList =
						new LinkedList<ServerInfo>();
					NodeList servers = child.getChildNodes();
					for (int j = 0; j < servers.getLength(); j++)
					{
						if (!(servers.item(j) instanceof Element))
						{
							continue;
						}
						Element server = (Element)servers.item(j);
						if (!server.getTagName().equals("server"))
						{
							throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
								"The <servers> tag may only contain <server> tags.");
						}
						if ("y".equals(server.getAttribute("this")))
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
				else if (child.getTagName().equals("eventthreads"))
				{
					try
					{
						eventThreads = Integer.parseInt(getText(child));
						if (eventThreads < 1 || eventThreads > 100)
						{
							throw new NumberFormatException();
						}
					}
					catch (NumberFormatException e)
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"The <eventthreads> value is not a valid thread count number.");
					}
				}
				else if (child.getTagName().equals("testkey"))
				{
					String channel = child.getAttribute("channel"),user =
						child.getAttribute("user"),displayName =
						child.getAttribute("displayname");
					if (channel == null
						|| (!channel.matches(Hawthorn.REGEXP_USERCHANNEL) && !channel
							.equals(Logger.SYSTEMLOG)))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires channel=, with no special characters");
					}
					if (user == null || !user.matches(Hawthorn.REGEXP_USERCHANNEL))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires user=, with no special characters");
					}
					if (displayName == null
						|| !displayName.matches(Hawthorn.REGEXP_DISPLAYNAME))
					{
						throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
							"<testkey> requires displayname=, with no double quotes");
					}
					testKeys.add(new TestKey(channel, user, displayName));
				}
				else
				{
					throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
						"The tag <" + child.getTagName() + "> is not recognised.");
				}
			}

			if (logFolder == null)
			{
				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Missing required <logfolder> configuration element. Specify a "
						+ "folder where logs should be stored. Example:\n\n"
						+ "<logfolder>/var/logs/jschat</logfolder>");
			}
			if (magicNumber == null)
			{
				MessageDigest m;
				try
				{
					m = MessageDigest.getInstance("SHA-1");
				}
				catch (NoSuchAlgorithmException e)
				{
					throw new StartupException(ErrorCode.STARTUP_MISSINGSHA1,
						"The SHA1 hash algorithm is not available in this Java "
							+ "installation. Check you are using an appropriate Java "
							+ "runtime.");
				}
				String s = Math.random() + "";
				m.update(s.getBytes(), 0, s.length());
				String sha1 = new BigInteger(1, m.digest()).toString(16);
				while (sha1.length() < 40)
				{
					sha1 = "0" + sha1;
				}

				throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
					"Missing required <magicnumber> configuration element. Specify a "
						+ "suitable random string that will be kept secure. Here's one "
						+ "generated for you:\n\n<magicnumber>" + sha1 + "</magicnumber>");
			}
			if (thisServer == null)
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
			logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL,
				"Hawthorn system startup");

			logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL, "Log folder: "
				+ logFolder);
			logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL, "Log level: "
				+ minLogLevel);
			logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL, "Logs deleted after: "
				+ (logDays == 0 ? "never" : logDays + " days"));

			logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL,
				"History retained for: " + historyHours + " hours");

			logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL, "This server: "
				+ thisServer);
			for (int i = 0; i < otherServers.length; i++)
			{
				logger.log(Logger.SYSTEMLOG, Logger.Level.NORMAL, "Remote server: "
					+ otherServers[i]);
			}
		}
		catch (ParserConfigurationException e)
		{
			throw new StartupException(ErrorCode.STARTUP_XMLSYSTEM,
				"The XML system appears to be incorrectly configured.");
		}
		catch (SAXException e)
		{
			throw new StartupException(ErrorCode.STARTUP_CONFIGFORMAT,
				"The configuration file is not valid XML.", e);
		}
		catch (IOException e)
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
		private String channel, user, displayName;

		/**
		 * @param channel Channel name
		 * @param user User name
		 * @param displayName Display name
		 */
		TestKey(String channel, String user, String displayName)
		{
			this.channel = channel;
			this.user = user;
			this.displayName = displayName;
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
				System.out.println("        Time: " + testKeyTime);
				System.out.println();
				String key =
					app.getValidKey(channel, user, displayName, testKeyTime);
				System.out.println("         Key: " + key);
				System.out.println();
				System.out.println("channel=" + channel + "&user=" + user
					+ "&displayname=" + URLEncoder.encode(displayName, "UTF-8")
					+ "&keytime=" + testKeyTime + "&key=" + key);
				System.out.println();
			}
			catch (Exception e)
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

	/** @return History time to keep messages for */
	public int getHistoryHours()
	{
		return historyHours;
	}

	/** @return True if chat messages should be logged at this server */
	public boolean logChat()
	{
		return logChat;
	}

	/**
	 * Checks whether a given address belongs to one of the other servers.
	 *
	 * @param possible Address that might be another server
	 * @return True if it is
	 */
	public boolean isOtherServer(InetAddress possible)
	{
		for (ServerInfo compare : otherServers)
		{
			if (compare.getAddress().getHostAddress().equals(
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