/*
 * Copyright 2009 Samuel Marshall http://www.leafdigital.com/software/hawthorn/
 *
 * This file is part of hawthorn.
 *
 * hawthorn is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * hawthorn is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * hawthorn. If not, see <http://www.gnu.org/licenses/>.
 */
package com.leafdigital.hawthorn;

import java.util.*;

import com.leafdigital.hawthorn.HttpServer.Connection;

/** Channel. */
public class Channel extends HawthornObject
{
	private String name;

	/** Indicates that any time/age/number of messages is acceptable */
	public final static int ANY = -1;

	/** Time to wait for new messages before closing connection */
	private final static int WAITTIME = 60 * 1000;

	/** Time to wait with no activity before assuming a user is absent */
	private final static int PRESENT_TIMEOUT = 90 * 1000;

	private final static Message[] NOMESSAGES = {};

	/**
	 * List of messages remembered in the channel; it remembers messages for a
	 * certain time
	 */
	private LinkedList<Message> messages = new LinkedList<Message>();

	/** List of listeners currently waiting for channel messages */
	private HashMap<Integer, Listener> listeners =
		new HashMap<Integer, Listener>();

	/** Index into listeners by user ID */
	private HashMap<String, LinkedList<Listener>> listenersByUser =
		new HashMap<String, LinkedList<Listener>>();


	/** Map from user ID of everyone present in the channel */
	private HashMap<String, UserInfo> present = new HashMap<String, UserInfo>();

	private long lastMessage = 0;

	/** Class handles a user/connection listening for an upcoming message. */
	private class Listener extends Event
	{
		private Connection connection;

		private int timeoutId;

		private long time;

		private String id;

		private String user;

		/**
		 * Constructs and starts waiting for timeout or message.
		 *
		 * @param app Main app object
		 * @param connection Connection for response
		 * @param user User ID
		 * @param id ID string
		 * @param time Time we started waiting for messages
		 */
		private Listener(Hawthorn app, Connection connection, String user,
			String id, long time)
		{
			super(app);
			this.connection = connection;
			this.time = time;
			this.id = id;
			this.user = user;

			// Set timer to wait for the event
			timeoutId =
				getEventHandler().addTimedEvent(System.currentTimeMillis() + WAITTIME,
					this);

			// Add this to the listeners list
			listeners.put(timeoutId, this);

			// And add it to the per-user index
			LinkedList<Listener> existing = listenersByUser.get(user);
			if (existing == null)
			{
				existing = new LinkedList<Listener>();
				listenersByUser.put(user, existing);
			}
			existing.add(this);
		}

		@Override
		/*
		 * * Called when the timeout occurs.
		 */
		public void handle() throws OperationException
		{
			// Remove from list of listeners
			synchronized (Channel.this)
			{
				// If item wasn't in list anyway, we've already responded, so don't
				// do anything else
				if (listeners.remove(timeoutId) == null)
				{
					return;
				}
			}

			// Send the no-response message
			sendWaitForMessageResponse(connection, id, time, NOMESSAGES, null);
		}

		/**
		 * Called when one or more new messages are received.
		 *
		 * @param messages Messages
		 */
		private void newMessages(Message[] messages)
		{
			synchronized (Channel.this)
			{
				// If item wasn't in list anyway, we've already responded, so don't
				// do anything else
				if (listeners.remove(timeoutId) == null)
				{
					return;
				}
				// Remove from by-user index
				LinkedList<Listener> byUser = listenersByUser.get(user);
				if (byUser != null) // It should not be null, but let's play safe
				{
					byUser.remove(this);
					if (byUser.isEmpty())
					{
						listenersByUser.remove(user);
					}
				}
			}

			// Remove event
			getEventHandler().removeTimedEvent(timeoutId);

			// Send response
			sendWaitForMessageResponse(connection, id, messages[messages.length - 1]
				.getTime(), messages, null);
		}
	}

	/**
	 * Details about a user who's currently in the channel.
	 */
	private class UserInfo extends Name
	{
		private boolean thisServer;

		private String ip;

		private long lastAccess;

		/**
		 * @param thisServer True if user is connected to this server
		 * @param ip IP address
		 * @param user User ID
		 * @param displayName Display name
		 */
		private UserInfo(boolean thisServer, String ip, String user,
			String displayName)
		{
			super(user, displayName);
			this.thisServer = thisServer;
			this.ip = ip;
			access();
		}

		/** Updates access time to now */
		void access()
		{
			lastAccess = System.currentTimeMillis();
		}

		/** @return True if user has timed out (only applies on owning server) */
		boolean timedOut()
		{
			return thisServer
				&& lastAccess + PRESENT_TIMEOUT < System.currentTimeMillis();
		}

		/** @return Suitable LeaveMessage representing a timeout */
		LeaveMessage newLeaveMessage()
		{
			return new LeaveMessage(System.currentTimeMillis(), getName(), ip,
				getUser(), getDisplayName(), true);
		}
	}

	/**
	 * @param app App main object
	 * @param name Name of channel
	 */
	public Channel(Hawthorn app, String name)
	{
		super(app);
		this.name = name;
	}

	/** @return Channel name */
	public String getName()
	{
		return name;
	}

	/**
	 * Cleans up old messsages and sends leave message for timed-out users.
	 *
	 * @return True if there are no messages in this channel and nobody is
	 *         listening so it should be thrown away
	 */
	synchronized boolean cleanup()
	{
		// Remove old messages
		long then =
			System.currentTimeMillis()
				- (getConfig().getHistoryHours() * 60L * 60L * 1000L);
		for (Iterator<Message> i = messages.iterator(); i.hasNext();)
		{
			Message m = i.next();
			if (m.getTime() > then)
			{
				break;
			}
			i.remove();
		}

		// Remove users in present list who have timed out
		LinkedList<UserInfo> timedOut = new LinkedList<UserInfo>();
		for (UserInfo info : present.values())
		{
			if (info.timedOut())
			{
				timedOut.add(info);
			}
		}
		for (UserInfo info : timedOut)
		{
			LeaveMessage leave = info.newLeaveMessage();
			getApp().getOtherServers().sendMessage(leave);
			message(leave, false);
		}

		// If there are no messages and listeners, OK to delete this channel
		return messages.isEmpty() && listeners.isEmpty();
	}

	/**
	 * Called when a user (local or remote) says something in the channel
	 *
	 * @param m Message
	 * @param remote True if this was from a remote user
	 */
	public synchronized void message(Message m, boolean remote)
	{
		// Message array
		Message[] newMessages =
		{
			m
		};

		// Handle presence information
		if (m instanceof SayMessage)
		{
			UserInfo existing = present.get(m.getUser());
			if (existing == null)
			{
				// User said something, so must be present
				Message join =
					new JoinMessage(System.currentTimeMillis(), getName(), m.getIP(), m
						.getUser(), m.getDisplayName());

				// Note that this autogenerated join message does not need to be
				// sent to the other servers.
				newMessages = new Message[]
				{
					join, m
				};

				// Add to presence list
				present.put(m.getUser(), new UserInfo(!remote, m.getIP(), m.getUser(),
					m.getDisplayName()));
			}
			else
			{
				// Mark that the user said something
				existing.access();
			}
		}
		else if (m instanceof JoinMessage)
		{
			UserInfo existing = present.get(m.getUser());
			if (existing == null)
			{
				// Add to presence list
				present.put(m.getUser(), new UserInfo(!remote, m.getIP(), m.getUser(),
					m.getDisplayName()));
			}
			else
			{
				// Already joined, so don't send message to listeners
				return;
			}
		}
		else if (m instanceof LeaveMessage)
		{
			// Remove from presence list
			if (present.remove(m.getUser()) == null)
			{
				// They weren't there? Then don't pass on message
				return;
			}

			// Note the listeners will automatically be closed by sending this
			// leave message.
		}

		internalMessage(newMessages);
	}

	private synchronized void internalMessage(Message[] newMessages)
	{
		for (Message m : newMessages)
		{
			// Ensure that no two messages have the same time
			if (m.getTime() == lastMessage)
			{
				m.setTime(lastMessage + 1);
			}

			// Add message
			messages.add(m);
			lastMessage = m.getTime();

			// Put message in chat logs if this server is logging
			if (getConfig().logChat())
			{
				getLogger().log(getName(), Logger.Level.NORMAL, m.getLogFormat());
			}
		}

		// Pass message(s) to all listeners
		while (!listeners.isEmpty())
		{
			// This sends the message(s) and removes the listener
			listeners.values().iterator().next().newMessages(newMessages);
		}
	}

	/**
	 * @param maxAge Maximum age in milliseconds
	 * @param maxNumber Maximum number of messages
	 * @return Array of messages that match the criteria
	 */
	public synchronized Message[] getRecent(int maxAge, int maxNumber)
	{
		long then = System.currentTimeMillis() - maxAge;
		int count = 0;
		ListIterator<Message> iterator = messages.listIterator(messages.size());
		while (iterator.hasPrevious() && count < maxNumber)
		{
			Message m = iterator.previous();
			if (m.getTime() < then)
			{
				iterator.next();
				break;
			}
			count++;
		}

		Message[] result = new Message[count];
		for (int i = 0; i < count; i++)
		{
			result[i] = iterator.next();
		}

		return result;
	}

	/**
	 * @param maxNames Maximum number of names to return or ANY
	 * @return Names of channel users (note: selection is arbitrary if there are
	 *         more than maxNames)
	 */
	public synchronized Name[] getNames(int maxNames)
	{
		Name[] result;
		if (present.size() < maxNames || maxNames == ANY)
		{
			result = new Name[present.size()];
		}
		else
		{
			result = new Name[maxNames];
		}
		Iterator<UserInfo> iterator = present.values().iterator();
		for (int i = 0; i < result.length; i++)
		{
			result[i] = iterator.next();
		}

		return result;
	}

	/**
	 * Registers a request for messages on this channel.
	 *
	 * @param connection Connection that wants to receive messages
	 * @param user User ID
	 * @param displayName User display name
	 * @param id ID number as string
	 * @param lastTime Time of last message (receives any messages with time
	 *        greater than this) or ANY
	 * @param maxAge Maximum age in milliseconds or ANY
	 * @param maxNumber Maximum number of messages or ANY
	 * @throws IllegalArgumentException If you specify too many parameters
	 */
	public synchronized void waitForMessage(Connection connection, String user,
		String displayName, String id, long lastTime, int maxAge, int maxNumber)
		throws IllegalArgumentException
	{
		if (lastTime != ANY && (maxAge != ANY || maxNumber != ANY))
		{
			throw new IllegalArgumentException("You can specify either lastTime "
				+ "or the maxAge/Number requirements, not both");
		}
		if (lastTime == ANY && (maxAge == ANY || maxNumber == ANY))
		{
			throw new IllegalArgumentException("If not specifying lastTime "
				+ "you must specify both maxAge and maxNumber");
		}

		// Looking for any recent messages (limited time/number)
		if (lastTime == ANY)
		{
			// If there are some recent messages, send them, otherwise just send
			// the name list anyhow
			Message[] recent = getRecent(maxAge, maxNumber);
			if (recent.length != 0)
			{
				sendWaitForMessageResponse(connection, id, recent[recent.length - 1]
					.getTime(), recent, getNames(ANY));
				return;
			}

			// Otherwise, wait for messages from now
			lastTime = System.currentTimeMillis() - 1;
		}
		else
		{
			// Looking for messages since the specified time
			ListIterator<Message> iterator = messages.listIterator(messages.size());
			int count = 0;
			while (iterator.hasPrevious())
			{
				Message m = iterator.previous();
				if (m.getTime() <= lastTime)
				{
					iterator.next();
					break;
				}
				count++;
			}

			if (count > 0)
			{
				Message[] result = new Message[count];
				for (int i = 0; i < count; i++)
				{
					result[i] = iterator.next();
				}
				sendWaitForMessageResponse(connection, id, result[result.length - 1]
					.getTime(), result, null);
				// When it responds straight away, we don't need for the user to
				// be present, because this is only equivalent to getRecent anyhow.
				// If they're really in the channel they will send another request
				// immediately.
				return;
			}
		}

		// Keep waiting for new messages
		new Listener(getApp(), connection, user, id, lastTime);

		// User is now present in channel
		UserInfo existing = present.get(user);
		if (existing == null)
		{
			String ip = connection.toString();

			// Send a join message to local and remote servers
			JoinMessage join =
				new JoinMessage(System.currentTimeMillis() + 1, getName(), ip, user,
					displayName);
			getApp().getOtherServers().sendMessage(join);
			message(join, false);
		}
		else
		{
			// Stave off the timeout
			existing.access();
		}
	}

	private void sendWaitForMessageResponse(Connection connection, String id,
		long lastTime, Message[] messages, Name[] names)
	{
		StringBuilder output = new StringBuilder();
		output.append("hawthorn.waitForMessageComplete(");
		output.append(id);
		output.append(',');
		output.append(lastTime);
		output.append(",[");
		for (int i = 0; i < messages.length; i++)
		{
			if (i != 0)
			{
				output.append(',');
			}
			output.append(messages[i].getJSFormat());
		}
		output.append("],[");
		if (names != null)
		{
			for (int i = 0; i < names.length; i++)
			{
				if (i != 0)
				{
					output.append(',');
				}
				output.append(names[i].getJSFormat());
			}
		}
		output.append("]);");
		connection.send(output.toString());
	}
}
