package com.leafdigital.hawthorn;

import java.util.HashMap;

/** List of current channels. */
public class Channels extends HawthornObject
{
	private final static int CHANNELDUMPFREQUENCY=10*60*1000;
	
	/** Stores data about each available channel. */
	private HashMap<String,Channel> channels=new HashMap<String,Channel>();
	
	private Object channelDumpSynch=new Object();
	private boolean close,closed;
	
	/** @param app Hawthorn app main object */
	public Channels(Hawthorn app)
	{
		super(app);
		
		new ChannelDumpThread();		
	}
	
	/**
	 * Gets a channel. If it's not in memory, creates it.
	 * @param name Name of desired channel
	 * @return Channel
	 */
	public Channel get(String name)
	{
		synchronized(channels)
		{
			Channel c=channels.get(name);
			if(c==null)
			{
				c=new Channel(getApp(),name);
				channels.put(name,c);
			}
			return c;
		}
	}
	
	/** Closes thread and bails. */
	public void close()
	{
		synchronized(channelDumpSynch)
		{
			close=true;
			while(!closed)
			{
				try
				{
					channelDumpSynch.wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}
	}
	
	/** Thread that discards unused channel objects and old messages. */
	private final class ChannelDumpThread extends Thread
	{
		ChannelDumpThread()
		{
			super("Channel dump thread");
			setPriority(Thread.MIN_PRIORITY);
			start();
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				// Wait
				synchronized(channelDumpSynch)
				{
					try
					{
						channelDumpSynch.wait(CHANNELDUMPFREQUENCY);
					}
					catch(InterruptedException e)
					{
					}
					
					if(close)
					{
						closed=true;
						channelDumpSynch.notifyAll();
						return;
					}					
				}
				
				// Get channels (then leave channel synch)
				Channel[] allChannels;
				synchronized(channels)
				{
					allChannels=channels.values().toArray(new Channel[channels.values().size()]);					
				}
				
				// Clean up all channels
				int count=allChannels.length;
				for(Channel channel : allChannels)
				{
					if(channel.cleanup())
					{
						synchronized(channels)
						{
							channels.remove(channel.getName());
							count--;
						}
					}
				}
				
				// Log status
				getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
					"Channel stats: channels open "+count);
			}
			
		}
	}

}
