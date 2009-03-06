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

import java.util.*;

import com.leafdigital.hawthorn.HttpServer.Connection;

/** Channel. */
public class Channel extends HawthornObject
{
	private String name;
	
	/** Indicates that any time/age/number of messages is acceptable */
	public final static int ANY=-1;
	
	/** Time to wait for new messages before closing connection */
	private final static int WAITTIME=60*1000;
	
	private final static Message[] NOMESSAGES={};
	
	private LinkedList<Message> messages=new LinkedList<Message>();
	
	private HashMap<Integer,Listener> listeners=new HashMap<Integer,Listener>();
	
	private long lastMessage=0;

	/** Class handles a user/connection listening for an upcoming message. */
	private class Listener extends Event
	{
		private Connection connection;
		private int timeoutId;
		private long time;
		private String id;
		
		/**
		 * Constructs and starts waiting for timeout or message.
		 * @param app Main app object
		 * @param connection Connection for response
		 * @param id ID string
		 * @param time Time we started waiting for messages
		 */
		private Listener(Hawthorn app,Connection connection,String id,long time)
		{
			super(app);
			this.connection=connection;
			this.time=time;
			this.id=id;
			
			// Set timer to wait for the event
			timeoutId=getEventHandler().addTimedEvent(
				System.currentTimeMillis()+WAITTIME,this);
			
			// Add this to the listeners list
			listeners.put(timeoutId,this);			
		}
		
		@Override
		/**
		 * Called when the timeout occurs.
		 */
		public void handle() throws OperationException
		{
			// Remove from list of listeners
			synchronized(Channel.this)
			{
				// If item wasn't in list anyway, we've already responded, so don't
				// do anything else
				if(listeners.remove(timeoutId)==null) return;
			}
			
			// Send the no-response message
			sendWaitForMessageResponse(connection,id,time,NOMESSAGES);
  	}

		/**
		 * Called when a new message is received.
		 * @param m Message
		 */
		private void newMessage(Message m)		
		{
			synchronized(Channel.this)
			{
				// If item wasn't in list anyway, we've already responded, so don't
				// do anything else
				if(listeners.remove(timeoutId)==null) return;
			}
			
			// Remove event
			getEventHandler().removeTimedEvent(timeoutId);
			
			// Send response
			sendWaitForMessageResponse(connection,id,m.getTime(),new Message[] {m});
		}
	}
	
	/**
	 * @param app App main object
	 * @param name Name of channel
	 */
	public Channel(Hawthorn app,String name)
	{
		super(app);
		this.name=name;
	}

	/** @return Channel name */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Cleans up old messsages.
	 * @return True if there are no messages in this channel and nobody is 
	 *   listening so it should be thrown away
	 */
	synchronized boolean cleanup()
	{
		long then=System.currentTimeMillis()-
			(long)(getConfig().getHistoryHours()*60L*60L*1000L);
		for(Iterator<Message> i=messages.iterator();i.hasNext();)
		{	
			Message m=i.next();
			if(m.getTime()>then) break;
			i.remove();
	  }
		
		// TODO Also check listeners
		return messages.isEmpty();
	}

	/**
	 * Called when a user (local or remote) says something in the channel
	 * @param m Message
	 */
	public synchronized void say(Message m)
	{
		// Ensure that no two messages have the same time
		if(m.getTime()==lastMessage)
			m.setTime(lastMessage+1);

		// Add message
		messages.add(m);
		lastMessage=m.getTime();
		
		// Put message in chat logs if this server is logging
		if(getConfig().logChat())
		{
			getLogger().log(getName(),Logger.Level.NORMAL,m.getIP()+" "+
				m.getUser()+" \""+m.getDisplayName()+"\" "+m.getMessage());
		}
		
		// Pass message to all listeners
		while(!listeners.isEmpty())
		{
			// This sends the message and removes the listener
			listeners.values().iterator().next().newMessage(m);
		}
	}

	/**
	 * @param maxAge Maximum age in milliseconds
	 * @param maxNumber Maximum number of messages
	 * @return Array of messages that match the criteria
	 */
	public synchronized Message[] getRecent(int maxAge,int maxNumber)
	{
		long then=System.currentTimeMillis()-(long)maxAge;
		int count=0;
		ListIterator<Message> iterator=messages.listIterator(messages.size());
		while(iterator.hasPrevious() && count<maxNumber)
		{
			Message m=iterator.previous();
			if(m.getTime()<then) 
			{
				iterator.next();
				break;
			}
			count++;
		}
		
		Message[] result=new Message[count];
		for(int i=0;i<count;i++)
		{
			result[i]=iterator.next();
		}
		
		return result;
	}

	/**
	 * Registers a request for messages on this channel.
	 * @param connection Connection that wants to receive messages
	 * @param id ID number as string
	 * @param lastTime Time of last message (receives any messages with time
	 *   greater than this) or ANY
	 * @param maxAge Maximum age in milliseconds or ANY
	 * @param maxNumber Maximum number of messages or ANY
	 * @throws IllegalArgumentException If you specify too many parameters
	 */
	public synchronized void waitForMessage(Connection connection,String id,
		long lastTime,int maxAge,int maxNumber) throws IllegalArgumentException
	{
		if(lastTime!=ANY && (maxAge!=ANY || maxNumber!=ANY))
			throw new IllegalArgumentException("You can specify either lastTime " +
				"or the maxAge/Number requirements, not both");
		if(lastTime==ANY && (maxAge==ANY || maxNumber==ANY))
			throw new IllegalArgumentException("If not specifying lastTime " +
				"you must specify both maxAge and maxNumber");

		// Looking for any recent messages (limited time/number)
		if(lastTime==ANY)
		{
			// If there are some recent messages, send them
			Message[] recent=getRecent(maxAge,maxNumber);
			if(recent.length!=0)
			{
				sendWaitForMessageResponse(
					connection,id,recent[recent.length-1].getTime(),recent);
				return;				
			}
			
			// Otherwise, wait for messages from now
			lastTime=System.currentTimeMillis()-1;
		}
		else
		{
			// Looking for messages since the specified time
			ListIterator<Message> iterator=messages.listIterator(messages.size());
			int count=0;
			while(iterator.hasPrevious())
			{
				Message m=iterator.previous();
				if(m.getTime()<=lastTime)
				{
					iterator.next();
					break;
				}
				count++;
			}
			
			if(count>0)
			{
				Message[] result=new Message[count];
				for(int i=0;i<count;i++)
				{
					result[i]=iterator.next();
				}
				sendWaitForMessageResponse(
					connection,id,result[result.length-1].getTime(),result);
				return;
			}
		}
		
		// Keep waiting for new messages
		new Listener(getApp(),connection,id,lastTime);
	}
	
	private void sendWaitForMessageResponse(Connection connection,
		String id,long lastTime,Message[] messages)
	{
		StringBuilder output=new StringBuilder();
		output.append("hawthorn.waitForMessageComplete(");
		output.append(id);
		output.append(',');
		output.append(lastTime);
		output.append(",[");
		for(int i=0;i<messages.length;i++)
		{
			if(i!=0) output.append(',');
			output.append(messages[i].getJS());
		}
		output.append("]);");
    connection.send(output.toString());
	}
}
