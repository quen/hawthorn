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

import java.util.*;

/** Event handler that dispatches events from a queue to multiple threads. */
public class EventHandler extends HawthornObject
	implements Statistics.InstantStatisticHandler
{
	private final static String STATISTIC_EVENT_QUEUE_SIZE = "EVENT_QUEUE_SIZE";

	private LinkedList<Event> queue = new LinkedList<Event>();
	private TreeSet<TimedEvent> timerQueue = new TreeSet<TimedEvent>();
	private boolean close;
	private boolean timerClosed;
	private int openThreads;
	private int timedEventID;

	/** So that we can make events happen at a future time. */
	private static class TimedEvent implements Comparable<TimedEvent>
	{
		private int id;
		private long time;
		private Event e;

		private TimedEvent(int id, long time, Event e)
		{
			this.id = id;
			this.time = time;
			this.e = e;
		}

		public int compareTo(TimedEvent other)
		{
			return other == this ? 0 : time < other.time ? -1 : 1;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj == this;
		}

		/** @return Time event is supposed to happen */
		long getTime()
		{
			return time;
		}

		/** @return Event */
		Event getEvent()
		{
			return e;
		}

		/** @return ID */
		public int getId()
		{
			return id;
		}
	}

	/**
	 * Constructs event queue and starts threads.
	 *
	 * @param app Application main class
	 */
	public EventHandler(Hawthorn app)
	{
		super(app);

		getStatistics().registerInstantStatistic(STATISTIC_EVENT_QUEUE_SIZE,this);

		int threads = getConfig().getEventThreads();
		for (int i = 0; i < threads; i++)
		{
			new EventThread(i);
		}
		new TimerThread();
		openThreads = threads;
	}

	/**
	 * Adds an event to the queue.
	 *
	 * @param e Event to add
	 */
	public void addEvent(Event e)
	{
		synchronized (queue)
		{
			// Add event to queue
			queue.addLast(e);

			// Wake one thread to consider it
			queue.notify();
		}
	}

	/**
	 * Adds an event to happen at a future date.
	 *
	 * @param time Time to happen
	 * @param e Event to add
	 * @return ID of timed event
	 */
	public int addTimedEvent(long time, Event e)
	{
		synchronized (timerQueue)
		{
			int id = ++timedEventID;
			timerQueue.add(new TimedEvent(id, time, e));
			timerQueue.notify();
			return id;
		}
	}

	/**
	 * Removes a timed event, if it is present. (Does not give an error if it's
	 * not present. Note that there are likely to be timing issues with removing
	 * events; i.e. when removing an event, you should be prepared for it to still
	 * occur.)
	 *
	 * @param id ID of event to remove
	 */
	public void removeTimedEvent(int id)
	{
		synchronized (timerQueue)
		{
			for (Iterator<TimedEvent> i = timerQueue.iterator(); i.hasNext();)
			{
				if (i.next().getId() == id)
				{
					i.remove();
				}
			}
		}
	}

	/**
	 * Shuts down the event handler, closing threads.
	 */
	public void close()
	{
		close = true;

		synchronized (timerQueue)
		{
			timerQueue.notifyAll();
			while (!timerClosed)
			{
				try
				{
					timerQueue.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}

		synchronized (queue)
		{
			queue.notifyAll();
			while (openThreads > 0)
			{
				try
				{
					queue.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	/** Thread that works on events. */
	private class EventThread extends Thread
	{
		EventThread(int index)
		{
			super("Event thread " + index);
			start();
		}

		@Override
		public void run()
		{
			while (true)
			{
				// Get next event to handle
				Event next;
				synchronized (queue)
				{
					while (queue.isEmpty())
					{
						// If close requested, abort
						if (close)
						{
							openThreads--;
							queue.notifyAll();
							return;
						}

						// Wait for event
						try
						{
							queue.wait();
						}
						catch (InterruptedException e)
						{
						}
					}

					// Get first event from queue
					next = queue.removeFirst();
				}

				// Handle event
				try
				{
					next.handle();
				}
				catch (Throwable t)
				{
					getLogger().log(Logger.SYSTEM_LOG, Logger.Level.ERROR,
						"Event processing error (" + getName() + ")", t);
				}
			}
		}
	}

	private class TimerThread extends Thread
	{
		TimerThread()
		{
			super("Event timer thread");
			// Increase priority so that the timed events get into the normal queue
			// ASAP and are therefore counted in the 'waiting' stats
			setPriority(Thread.NORM_PRIORITY + 1);
			start();
		}

		@Override
		public void run()
		{
			while (true)
			{
				synchronized (timerQueue)
				{
					long now = System.currentTimeMillis();
					long next = -1;

					// Remove any queue events that are present
					for (Iterator<TimedEvent> i = timerQueue.iterator(); i.hasNext();)
					{
						TimedEvent upcoming = i.next();
						if (upcoming.getTime() > now)
						{
							next = upcoming.getTime();
							break;
						}

						addEvent(upcoming.getEvent());
						i.remove();
					}

					// Wait until the next event
					try
					{
						if (next == -1)
						{
							timerQueue.wait();
						}
						else
						{
							timerQueue.wait(next - now);
						}
					}
					catch (InterruptedException e)
					{
					}

					// Check if we've been asked to close
					if (close)
					{
						timerClosed = true;
						timerQueue.notifyAll();
						return;
					}
				}
			}
		}
	}

	public int getValue()
	{
		synchronized (queue)
		{
			return queue.size();
		}
	}
}
