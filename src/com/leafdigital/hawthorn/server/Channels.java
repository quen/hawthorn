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

import java.util.HashMap;

/** List of current channels. */
public class Channels extends HawthornObject
{
	private final static int CHANNEL_DUMP_FREQUENCY = 60 * 1000;

	/** Stores data about each available channel. */
	private HashMap<String, Channel> channels = new HashMap<String, Channel>();

	private Object channelDumpSynch = new Object();
	private boolean close, closed;

	/** @param app Hawthorn app main object */
	public Channels(Hawthorn app)
	{
		super(app);

		new ChannelDumpThread();
	}

	/**
	 * Gets a channel. If it's not in memory, creates it.
	 *
	 * @param name Name of desired channel
	 * @return Channel
	 */
	public Channel get(String name)
	{
		synchronized (channels)
		{
			Channel c = channels.get(name);
			if (c == null)
			{
				c = new Channel(getApp(), name);
				channels.put(name, c);
			}
			return c;
		}
	}

	/** Closes thread and bails. */
	public void close()
	{
		synchronized (channelDumpSynch)
		{
			close = true;
			while (!closed)
			{
				try
				{
					channelDumpSynch.wait();
				}
				catch (InterruptedException e)
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
			while (true)
			{
				// Wait
				synchronized (channelDumpSynch)
				{
					try
					{
						channelDumpSynch.wait(CHANNEL_DUMP_FREQUENCY);
					}
					catch (InterruptedException e)
					{
					}

					if (close)
					{
						closed = true;
						channelDumpSynch.notifyAll();
						return;
					}
				}

				// Get channels (then leave channel synch)
				Channel[] allChannels;
				synchronized (channels)
				{
					allChannels =
						channels.values().toArray(new Channel[channels.values().size()]);
				}

				// Clean up all channels
				int count = allChannels.length;
				for (Channel channel : allChannels)
				{
					if (channel.cleanup())
					{
						synchronized (channels)
						{
							channels.remove(channel.getName());
							count--;
						}
					}
				}

				// Log status
				getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
					"Channel stats: channels open " + count);
			}

		}
	}

}
