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

import java.net.*;
import java.text.NumberFormat;
import java.util.*;

import com.leafdigital.hawthorn.server.Configuration.ServerInfo;
import com.leafdigital.hawthorn.util.XML;

/** Class that tracks statistics every minute. */
public class Statistics extends HawthornObject
{
	private Map<String, CountStatisticBunch> countStatistics =
		new TreeMap<String, CountStatisticBunch>();

	private Map<String, InstantStatisticBunch> instantStatistics =
		new TreeMap<String, InstantStatisticBunch>();

	private int currentHour, currentDay;

	private StatisticsEvent event;
	private int eventId;

	private final static String STATS_STYLE =
		"body { font: 11px Verdana, sans-serif; }" +
		"h1 { font-size: 100%; border-bottom: 1px solid #e9ddaf; " +
			"padding-bottom: 4px; }" +
		"h2 { font-size: 100%; }" +
		".basicstats { padding: 8px 0; }" +
		".basicstats .v { border:1px solid #e9ddaf; padding: 2px 4px; " +
			"margin-right: 8px; }" +
		".histogram { font-size: 8px; height: 114px; " +
			"margin: 4px 0; position:relative; }" +
		".bars { height: 100px; width: 660px; background: #e9ddaf; " +
			"position:absolute; top:0; left:0; }" +
		".b { position: absolute; bottom: 0px; width:20px; background: black; }" +
		".max { position: absolute; left: 660px; top:0; border-top:1px solid black; }" +
		".l { position: absolute; top: 100px; }" +
		".t { height:2px; border-left:1px solid black; }" +
		".d { position: absolute; top: 0; height:100px; width: 1px; " +
			"border-left: dotted 1px black; }";

	/**
	 * @param app Main app object
	 */
	protected Statistics(Hawthorn app)
	{
		super(app);
		event = new StatisticsEvent(app);
		eventId = -1;
	}

	/**
	 * Starts updating statistics (must be called after the event handler is
	 * available).
	 */
	void start()
	{
		Calendar calendar = Calendar.getInstance();
		currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		currentDay = calendar.get(Calendar.DAY_OF_MONTH);
		eventId = getEventHandler().addTimedEvent(getNextMinute(calendar), event);
	}

	/**
	 * Stops statistic-update events.
	 */
	void close()
	{
		if(eventId != -1)
		{
			getEventHandler().removeTimedEvent(eventId);
		}
	}

	/**
	 * @param calendar Calendar object with current time (will be changed!)
	 * @return Time in milliseconds of next minute
	 */
	private long getNextMinute(Calendar calendar)
	{
		calendar.set(Calendar.MILLISECOND,0);
		calendar.set(Calendar.SECOND,0);
		calendar.add(Calendar.MINUTE,1);
		return calendar.getTimeInMillis();
	}

	private class StatisticsEvent extends Event
	{
		/**
		 * @param app Main app object
		 */
		protected StatisticsEvent(Hawthorn app)
		{
			super(app);
		}

		@Override
		public void handle() throws OperationException
		{
			// Update time statistics
			for(Map.Entry<String, CountStatisticBunch> entry
				: countStatistics.entrySet())
			{
				CountStatistic minute = entry.getValue().getMinute();
				String log;
				synchronized(minute)
				{
					// Get current value to log
					log = minute.toString();

					// Add current value to the larger time periods
					entry.getValue().getHour().add(minute);
					entry.getValue().getDay().add(minute);

					// Clear stats again
					minute.clear();
				}
				getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
					"M Statistic ["+entry.getKey()+"] "+log);
			}

			// Update instant statistics
			for(Map.Entry<String, InstantStatisticBunch> entry
				: instantStatistics.entrySet())
			{
				entry.getValue().update();
				int value = entry.getValue().getLastValue();
				getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
					"M Statistic ["+entry.getKey()+"] "+value);
			}

			// Check if times have changed
			Calendar calendar = Calendar.getInstance();
			int thisHour = calendar.get(Calendar.HOUR_OF_DAY);
			int thisDay = calendar.get(Calendar.DAY_OF_MONTH);
			if(thisHour != currentHour)
			{
				// Update time statistics
				for(Map.Entry<String,CountStatisticBunch> entry : countStatistics.entrySet())
				{
					CountStatistic hour = entry.getValue().getHour();
					String log;
					synchronized(hour)
					{
						// Get current value to log
						log = hour.toString();

						// Clear stats again
						hour.clear();
					}
					getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
						"H Statistic [" + entry.getKey() + "] " + log);
				}

				// Update instant statistics
				for(Map.Entry<String, InstantStatisticBunch> entry
					: instantStatistics.entrySet())
				{
					BasicStatistic hour = entry.getValue().getHour();
					getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
						"H Statistic [" + entry.getKey() + "] " + hour.toString());
					hour.clear();
				}

				currentHour = thisHour;
			}

			if(thisDay != currentDay)
			{
				// Update time statistics
				for(Map.Entry<String,CountStatisticBunch> entry : countStatistics.entrySet())
				{
					CountStatistic day = entry.getValue().getDay();
					String log;
					synchronized(day)
					{
						// Get current value to log
						log = day.toString();

						// Clear stats again
						day.clear();
					}
					getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
						"D Statistic ["+entry.getKey()+"] "+log);
				}

				// Update instant statistics
				for(Map.Entry<String, InstantStatisticBunch> entry
					: instantStatistics.entrySet())
				{
					BasicStatistic day = entry.getValue().getDay();
					getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL,
						"D Statistic [" + entry.getKey() + "] " + day.toString());
					day.clear();
				}

				currentDay = thisDay;
			}

			// Add event again for next minute
			eventId = getEventHandler().addTimedEvent(getNextMinute(calendar), event);
		}
	}

	/**
	 * Registers a statistic so that the system will track it.
	 * @param name Name for statistic
	 */
	public void registerTimeStatistic(String name)
	{
		countStatistics.put(name, new CountStatisticBunch(
			new TimeStatistic(), new TimeStatistic(), new TimeStatistic()));
	}

	/**
	 * Adds an entry to a time statistic.
	 * @param name Name for statistic
	 * @param ms Time in milliseconds
	 * @throws IllegalArgumentException If the name is not registered or wrong
	 */
	public void updateTimeStatistic(String name, int ms)
		throws IllegalArgumentException
	{
		CountStatisticBunch collection = countStatistics.get(name);
		if (collection == null)
		{
			throw new IllegalArgumentException("Unknown time statistic: "+name);
		}
		CountStatistic minute = collection.getMinute();
		if (minute.getClass() != TimeStatistic.class)
		{
			throw new IllegalArgumentException("Not a time statistic: "+name);
		}
		((TimeStatistic)minute).add(ms);
	}

	/**
	 * Registers a statistic so that the system will track it.
	 * @param name Name for statistic
	 */
	public void registerCountStatistic(String name)
	{
		countStatistics.put(name, new CountStatisticBunch(
			new CountStatistic(), new CountStatistic(), new CountStatistic()));
	}

	/**
	 * Adds an entry to a count statistic.
	 * @param name Name for statistic
	 * @throws IllegalArgumentException If the name is not registered or wrong
	 */
	public void updateCountStatistic(String name)
	{
		CountStatisticBunch collection = countStatistics.get(name);
		if (collection == null)
		{
			throw new IllegalArgumentException("Unknown count statistic: "+name);
		}
		CountStatistic minute = collection.getMinute();
		if (minute.getClass() != CountStatistic.class)
		{
			throw new IllegalArgumentException("Not a count statistic: "+name);
		}
		minute.count();
	}

	/**
	 * Registers a statistic so that the system will track it.
	 * @param name Name for statistic
	 * @param handler Handler that provides value (will be called once per minute)
	 */
	public void registerInstantStatistic(String name,
		InstantStatisticHandler handler)
	{
		instantStatistics.put(name, new InstantStatisticBunch(handler));
	}

	/**
	 * Interface implemented by classes which provide 'instant' (evaluated
	 * once per minute) statistics.
	 */
	public interface InstantStatisticHandler
	{
		/**
		 * @return Current value of statistic
		 */
		public int getValue();
	}

	/**
	 * Stores information about an instant statistic.
	 */
	private static class InstantStatisticBunch
	{
		private InstantStatisticHandler handler;
		private int lastValue;

		private BasicStatistic
			hour = new BasicStatistic(),
			day = new BasicStatistic();

		/**
		 * @param handler Handler that returns values of this statistic
		 */
		public InstantStatisticBunch(InstantStatisticHandler handler)
		{
			this.handler = handler;
		}

		/** Updates statistics with a new value */
		void update()
		{
			lastValue = handler.getValue();
			hour.add(lastValue);
			day.add(lastValue);
		}

		/** @return Most recent value */
		int getLastValue()
		{
			return lastValue;
		}

		/** @return Statistics for current hour */
		public BasicStatistic getHour()
		{
			return hour;
		}

		/** @return Statistics for current day */
		public BasicStatistic getDay()
		{
			return day;
		}

		/** @return Handler */
		public InstantStatisticHandler getHandler()
		{
			return handler;
		}
	}

	/**
	 * Tracks a value that is updated periodically.
	 * <p>
	 * Note that this is not synchronized since it is only ever called from the
	 * statistics update event.
	 */
	private static class BasicStatistic
	{
		private int count, totalValue;

		/**
		 * Adds a new value to the statistic.
		 * @param value Value
		 */
		public void add(int value)
		{
			totalValue += value;
			count++;
		}

		/**
		 * Clears the values.
		 */
		public void clear()
		{
			count = 0;
			totalValue = 0;
		}

		/**
		 * Converts these statistics to a string suitable for including in logs.
		 * @return String containing statistical data
		 */
		@Override
		public String toString()
		{
			StringBuilder result = new StringBuilder();

			// Mean
			result.append("mean ");
			double average = (count == 0) ? 0.0 : (double)totalValue / (double)count;
			NumberFormat format = NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(1);
			result.append(format.format(average));

			return result.toString();
		}
	}

	/**
	 * Tracks statistics about events that occur periodically and take a
	 * certain amount of time. Best used for times between 0 and 5000ms.
	 */
	private static class TimeStatistic extends CountStatistic
	{
		private int totalTime;
		private int[] histogram = new int[33];

		/**
		 * Called whenever an event occurs.
		 * @param ms Event time in milliseconds
		 */
		public synchronized void add(int ms)
		{
			totalTime += ms;
			super.count();

			// Histogram stores >0ms, >1ms, >2ms, >3ms, >4ms...
			if (ms < 5)
			{
				histogram[ms]++;
			}
			// ...>5, >10, >15, >20, >25, >30, >35, >40, >45...
			else if (ms < 50)
			{
				histogram[ms/5 + 4]++;
			}
			// ...>50, >100, >150, >200, >250, >300, >350, >400, >450...
			else if (ms < 500)
			{
				histogram[ms/50 + 13]++;
			}
			// ...>500, >1000, >1500, >2000, >2500, >3000, >3500, >4000, >4500...
			else if (ms < 5000)
			{
				histogram[ms/500 + 22]++;
			}
			// ...>5000
			else
			{
				histogram[32]++;
			}
		}

		/**
		 * @throws UnsupportedOperationException You cannot call the base class
		 *   count() with a TimeStatistic
		 */
		@Override
		public synchronized void count()
		{
			throw new UnsupportedOperationException(
				"Cannot count with TimeStatistic");
		}

		@Override
		/** Clear these statistics. */
		public synchronized void clear()
		{
			super.clear();
			totalTime = 0;
			for (int i=0; i<histogram.length; i++)
			{
				histogram[i] = 0;
			}
		}

		/**
		 * Adds an entire other object to this one.
		 * @param other Object to add
		 */
		@Override
		public synchronized void add(CountStatistic other)
		{
			// Note: Since it is usually the 'minute' field being added, we
			// are actually already synchronized on it, and this is unlikely
			// to cause a deadlock (but it clears a FindBugs warning).
			synchronized(other)
			{
				super.add(other);
				TimeStatistic otherTime = (TimeStatistic)other;
				totalTime += otherTime.totalTime;
				for (int i=0; i<histogram.length; i++)
				{
					histogram[i] += otherTime.histogram[i];
				}
			}
		}

		/**
		 * Obtains a string description of the range covered by a given index
		 * in the histogram.
		 * @param index Index in histogram
		 * @return Description of range e.g. "100-150", up to "5000-"
		 */
		private static String getHistogramRange(int index)
		{
			if (index < 5)
			{
				return index + "";
			}
			return getHistogramRangeStart(index, false) + "-" +
				(index==33 ? "" : getHistogramRangeStart(index+1, false));
		}

		/**
		 * Obtains a string description of a single position in the histogram.
		 * @param index Index in histogram
		 * @param legend True if this is for the histogram legend (adds units)
		 * @return Description of position
		 *
		 */
		private static String getHistogramRangeStart(int index, boolean legend)
		{
			// Convert range to string
			if (index < 5)
			{
				if(legend && index==0)
				{
					return "0ms";
				}
				return ""+index;
			}
			else if (index < 14)
			{
				return ""+((index-4) * 5);
			}
			else if (index < 23)
			{
				return ""+((index-13) * 50);
			}
			else if (index < 32)
			{
				String milliseconds = ""+((index-22) * 500);
				if (legend)
				{
					if (index == 23)
					{
						return "0.5s";
					}
					return milliseconds.substring(0,1) + "." +milliseconds.substring(1,2);
				}
				else
				{
					return milliseconds;
				}
			}
			else
			{
				if(legend)
				{
					return "5.0";
				}
				return "5000";
			}
		}

		/**
		 * Obtains the median range. The result will be in a format such as
		 * "100-150" (milliseconds). If there is no data then the result will be
		 * "-" and the unbounded result is "5000-".
		 * @return A string describing the median range
		 */
		public synchronized String getMedian()
		{
			if (count == 0)
			{
				return "-";
			}
			int countSoFar = 0;
			for (int i=0; i<histogram.length; i++)
			{
				countSoFar += histogram[i];
				if (countSoFar > count/2)
				{
					return getHistogramRange(i);
				}
			}
			// It shouldn't be able to get here, but the compiler doesn't know that
			return getHistogramRange(histogram.length-1);
		}

		/**
		 * Converts these statistics to a string suitable for including in logs.
		 * @return String containing statistical data
		 */
		@Override
		public synchronized String toString()
		{
			StringBuilder result = new StringBuilder();

			// Count
			result.append(count);

			// Mean
			result.append(" mean ");
			double average = (count == 0) ? 0.0 : (double)totalTime / (double)count;
			NumberFormat format = NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(1);
			result.append(format.format(average));

			// Median
			result.append("ms median ");
			result.append(getMedian());

			// Histogram
			result.append("ms histogram [");
			for (int i=0; i<histogram.length; i++)
			{
				if (i!=0)
				{
					result.append(',');
				}
				result.append(histogram[i]);
			}
			result.append(']');

			return result.toString();
		}

		/**
		 * @return HTML version of the data
		 */
		@Override
		public synchronized String toHtml()
		{
			StringBuilder result = new StringBuilder();

			result.append("<div class='basicstats'><span class='n'>Count</span> ");
			result.append("<span class='v'>");
			result.append(count);
			result.append("</span> <span class='n'>Mean</span> <span class='v'>");
			double average = (count == 0) ? 0.0 : (double)totalTime / (double)count;
			NumberFormat format = NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(1);
			result.append(format.format(average));
			result.append("ms</span> <span class='n'>Median</span> <span class='v'>");
			result.append(getMedian());
			result.append("ms</span></div><div class='histogram'><div class='bars'>");

			// Get maximum for scaling
			int max = 0;
			for (int i=0; i<histogram.length; i++)
			{
				if (histogram[i] > max)
				{
					max = histogram[i];
				}
			}

			// Draw histogram bars
			int rounding = max / 2;
			for (int i=0; i<histogram.length; i++)
			{
				int val = max == 0 ? 0 : (histogram[i] * 100 + rounding) / max;
				result.append("<div class='b' style='left: ");
				result.append(i*20);
				result.append("px; height:");
				result.append(val);
				result.append("px'></div>");
			}

			// Draw dividing lines where step size changes
			int[] changes = { 5, 14, 23, 32 };
			for(int i : changes)
			{
				result.append("<div class='d' style='left: ");
				result.append(i*20);
				result.append("px;'></div>");
			}

			// Draw max scale
			result.append("</div><div class='max'>");
			result.append(max);
			result.append("</div>");

			// Draw legend
			for (int i=0; i<=histogram.length; i++)
			{
				result.append("<div class='l' style='left:");
				result.append(i*20);
				result.append("px'><div class='t'></div>");
				if (i==histogram.length)
				{
					result.append("&#x221e;");
				}
				else
				{
					result.append(getHistogramRangeStart(i, true));
				}
				result.append("</div>");
			}

			result.append("</div>");

			return result.toString();
		}
	}

	/**
	 * Holds the time statistics for various time periods.
	 */
	private static class CountStatisticBunch
	{
		private CountStatistic minute, hour, day;

		CountStatisticBunch(CountStatistic minute, CountStatistic hour,
			CountStatistic day)
		{
			this.minute = minute;
			this.hour = hour;
			this.day = day;
		}

		/** @return Statistics for current minute */
		public CountStatistic getMinute()
		{
			return minute;
		}

		/** @return Statistics for current hour */
		public CountStatistic getHour()
		{
			return hour;
		}

		/** @return Statistics for current day */
		public CountStatistic getDay()
		{
			return day;
		}
	}

	/** Statistic that counts events */
	private static class CountStatistic
	{
		protected int count;

		public synchronized void count()
		{
			count++;
		}

		public synchronized void clear()
		{
			count = 0;
		}

		/**
		 * Adds an entire other object to this one.
		 * @param other Object to add
		 */
		public synchronized void add(CountStatistic other)
		{
			// Note: Since it is usually the 'minute' field being added, we
			// are actually already synchronized on it, and this is unlikely
			// to cause a deadlock (but it clears a FindBugs warning).
			synchronized(other)
			{
				count += other.count;
			}
		}

		/**
		 * Converts these statistics to a string suitable for including in logs.
		 * @return String containing statistical data
		 */
		@Override
		public synchronized String toString()
		{
			return count+"";
		}

		/**
		 * @return HTML version of the data
		 */
		public synchronized String toHtml()
		{
			return count+"";
		}
	}


	/**
	 * Administration function to get statistics in HTML.
	 * @return An HTML summary of current statistics
	 */
	String getSummaryHtml()
	{
		StringBuilder output = new StringBuilder();

		output.append("<h2>Current minute</h2><ul>");

		// Get time statistics
		for(Map.Entry<String, CountStatisticBunch> entry
			: countStatistics.entrySet())
		{
			CountStatistic minute = entry.getValue().getMinute();
			String log = minute.toHtml();
			output.append("<li><strong>");
			output.append(XML.esc(entry.getKey()));
			output.append("</strong>: ");
			output.append(log);
			output.append("</li>");
		}

		output.append("</ul><p class='note'>");
		output.append("Hourly and daily stats do not include current minute.");

		output.append("</p><h2>Last minute (snapshot)</h2><ul>");

		// Get instant statistics
		for(Map.Entry<String, InstantStatisticBunch> entry
			: instantStatistics.entrySet())
		{
			int value = entry.getValue().getLastValue();
			output.append("<li><strong>");
			output.append(XML.esc(entry.getKey()));
			output.append("</strong>: ");
			output.append(value);
			output.append("</li>");
		}

		output.append("</ul><h2>Current hour</h2><ul>");

		// Get time statistics
		for(Map.Entry<String, CountStatisticBunch> entry
			: countStatistics.entrySet())
		{
			CountStatistic hour = entry.getValue().getHour();
			String log = hour.toHtml();
			output.append("<li><strong>");
			output.append(XML.esc(entry.getKey()));
			output.append("</strong>: ");
			output.append(log);
			output.append("</li>");
		}

		// Get instant statistic averages
		for(Map.Entry<String, InstantStatisticBunch> entry
			: instantStatistics.entrySet())
		{
			String log = entry.getValue().getHour().toString();
			output.append("<li><strong>");
			output.append(XML.esc(entry.getKey()));
			output.append("</strong>: ");
			output.append(log);
			output.append("</li>");
		}

		output.append("</ul><h2>Current day</h2><ul>");

		// Get time statistics
		for(Map.Entry<String, CountStatisticBunch> entry
			: countStatistics.entrySet())
		{
			CountStatistic day = entry.getValue().getDay();
			String log = day.toHtml();
			output.append("<li><strong>");
			output.append(XML.esc(entry.getKey()));
			output.append("</strong>: ");
			output.append(log);
			output.append("</li>");
		}

		// Get instant statistic averages
		for(Map.Entry<String, InstantStatisticBunch> entry
			: instantStatistics.entrySet())
		{
			String log = entry.getValue().getDay().toString();
			output.append("<li><strong>");
			output.append(XML.esc(entry.getKey()));
			output.append("</strong>: ");
			output.append(log);
			output.append("</li>");
		}

		output.append("</ul>");

		ServerInfo thisServer = getConfig().getThisServer();
		String serverDetails;
		String addressPort = thisServer.getAddress().getHostAddress() + ":"
			+ thisServer.getPort();
		try
		{
			String serverName = InetAddress.getLocalHost().getHostName();
			serverDetails = serverName + " (" + addressPort + ")";
		}
		catch (UnknownHostException e)
		{
			serverDetails = addressPort;
		}
		return XML.getXHTML(serverDetails	+ " - Hawthorn statistics",
			"<style type='text/css'>\n" + STATS_STYLE + "</style>", output.toString());
	}

}
