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

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import com.leafdigital.hawthorn.util.Auth;

/**
 * Server main object. Mainly just stores links to the key components that make
 * up the system.
 */
public class Hawthorn
{
	/** Configuration file details. */
	private Configuration config;

	/** Statistics data */
	private Statistics statistics;

	/** Server that handles requests. */
	private HttpServer server;

	/** Channel list */
	private Channels channels;

	/** Event-processing threads */
	private EventHandler eventHandler;

	/** Other server connections */
	private OtherServers otherServers;

	/** Statistic: current memory usage */
	private final static String STATISTIC_MEMORY_USAGE_KB = "MEMORY_USAGE_KB";

	// NOTE: The following two regexps are duplicated in JSP connector
	// (InitTag.java) and PHP connector.

	/** Regular expression for user or channel name: letters, numbers and _ */
	public static final String REGEXP_USERCHANNEL = "[A-Za-z0-9_]+";

	/** Regular expression for display name: all normal characters except " */
	public static final String REGEXP_DISPLAYNAME = "[^\u0000-\u001f\"]+";

	/**
	 * Regular expression for extra: all normal characters except ", may be
	 * empty
	 */
	public static final String REGEXP_EXTRA = "[^\u0000-\u001f\"]*";

	/** Regular expression for message: all normal characters */
	public static final String REGEXP_MESSAGE = "[^\u0000-\u001f]+";

	private Hawthorn(File configFile) throws StartupException
	{
		// Check SHA-1 support
		try
		{
			Auth.hash("anything");
		}
		catch(NoSuchAlgorithmException e)
		{
			throw new StartupException(ErrorCode.STARTUP_MISSINGSHA1,
				"The SHA-1 hash algorithm is not available in this Java "
				+ "installation. Check you are using an appropriate Java "
				+ "runtime.");
		}

		config = new Configuration(configFile);
		statistics = new Statistics(this);
		channels = new Channels(this);
		otherServers = new OtherServers(this);
		eventHandler = new EventHandler(this);
		server = new HttpServer(this);
		statistics.registerInstantStatistic(STATISTIC_MEMORY_USAGE_KB,
			new Statistics.InstantStatisticHandler()
			{
				public int getValue()
				{
					long usedMemory = Runtime.getRuntime().totalMemory()
						- Runtime.getRuntime().freeMemory();
					return (int)(usedMemory/1024L);
				}

			});
		statistics.start();

		if(config.getTestKeys().size() > 0)
		{
			System.out.println("http://"
				+ config.getThisServer().getAddress().getHostAddress() + ":"
				+ config.getThisServer().getPort() + "/");
			System.out.println();
		}
		for(Configuration.TestKey test : config.getTestKeys())
		{
			test.show(this);
		}
	}

	/**
	 * Initialises the system.
	 *
	 * @param args Command-line arguments; should be a single argument to the
	 *        config file
	 */
	public static void main(String[] args)
	{
		try
		{
			if(args.length != 1)
			{
				throw new StartupException(ErrorCode.STARTUP_COMMANDLINE,
					"Please start this server with a single "
						+ "command-line parameter pointing to the configuration file.");
			}
			new Hawthorn(new File(args[0]));
		}
		catch(StartupException e)
		{
			System.err.println(e);
		}
	}

	/** @return Configuration */
	public Configuration getConfig()
	{
		return config;
	}

	/** @return Channel list */
	public Channels getChannels()
	{
		return channels;
	}

	/** @return Event processor */
	public EventHandler getEventHandler()
	{
		return eventHandler;
	}

	/** @return Other-server handler */
	public OtherServers getOtherServers()
	{
		return otherServers;
	}

	/** @return Statistics system */
	public Statistics getStatistics()
	{
		return statistics;
	}

	/**
	 * Closes the app, shutting all threads.
	 */
	public void close()
	{
		statistics.close();
		server.close();
		channels.close();
		eventHandler.close();
		otherServers.close();
		config.getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
			"Hawthorn system closed down.");
		config.getLogger().close();
	}

	/**
	 * Obtains key to check against for authentication.
	 *
	 * @param channel Channel ID
	 * @param user User ID
	 * @param displayName User display name
	 * @param extra Extra user details
	 * @param permissions User permissions
	 * @param keyTime Key issue time
	 * @return Correct key
	 */
	String getValidKey(String channel, String user, String displayName,
		String extra,	EnumSet<Auth.Permission> permissions,	long keyTime)
	{
		try
		{
			return Auth.getKey(getConfig().getMagicNumber(), user, displayName,
				extra, permissions, channel, keyTime);
		}
		catch(NoSuchAlgorithmException e)
		{
			// This is really not expected as we tested during init
			throw new Error("Unexpected error: missing SHA-1");
		}
	}

	/**
	 * Uses the server's magic number to create a hashed version of a user ID
	 * which can be sent to people we don't trust to see their real user ID.
	 * @param user User ID
	 * @return Masked user ID (begins with ?)
	 */
	String getMaskedUser(String user)
	{
		try
		{
			return "?" + Auth.hash(getConfig().getMagicNumber() + "\n" + user);
		}
		catch(NoSuchAlgorithmException e)
		{
			// This is really not expected as we tested during init
			throw new Error("Unexpected error: missing SHA-1");
		}
	}
}
