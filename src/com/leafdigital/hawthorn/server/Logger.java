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
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

import com.leafdigital.hawthorn.util.JS;

/** Handles logfiles. */
public final class Logger
{
	/** Log filename used for system-related logging */
	public final static String SYSTEMLOG = "!system";

	private final static int LOGUPDATEFREQUENCY = 5000,
		DELETECHECKFREQUENCY = 60 * 60 * 1000, LOGSTAYOPEN = 2 * 60 * 1000;

	private int logDays;
	private Level showLevel;
	private File folder;
	private String address;

	private HashMap<File, OpenFile> openFiles = new HashMap<File, OpenFile>();

	private Object threadSynch = new Object();
	private boolean close = false, closed = false;

	/** Importance level of logged message. */
	public enum Level
	{
		/** Message should only be displayed when showing detailed logging. */
		DETAIL(0),
		/** Message indicates normal event. */
		NORMAL(100),
		/** Message indicates error. */
		ERROR(200),
		/** Message indicates fatal error. */
		FATALERROR(300);

		private int value;

		private Level(int value)
		{
			this.value = value;
		}

		/**
		 * @param min Minimum required level
		 * @return True if this level is at least the required one
		 */
		public boolean isAtLeast(Level min)
		{
			return value >= min.value;
		}
	}

	/** Represents a file that's currently open for writing. */
	private static class OpenFile
	{
		BufferedWriter writer;
		long lastWrite;
		boolean flushed;

		/**
		 * Sets up the open-file record.
		 *
		 * @param writer Writer for data
		 */
		OpenFile(BufferedWriter writer)
		{
			this.writer = writer;
			lastWrite = System.currentTimeMillis();
			flushed = true;
		}

		/**
		 * Writes a line to the log.
		 *
		 * @param line Line (not including terminating \n)
		 */
		synchronized void write(String line)
		{
			try
			{
				writer.write(line + "\n");
			}
			catch (IOException e)
			{
				System.err.println("Error writing to logfile:");
				e.printStackTrace();
			}
			lastWrite = System.currentTimeMillis();
			flushed = false;
		}

		/**
		 * Closes the log.
		 */
		synchronized void close()
		{
			try
			{
				writer.flush();
				writer.close();
				writer = null;
			}
			catch (IOException e)
			{
				System.err.println("Error closing logfile:");
				e.printStackTrace();
			}
		}

		/**
		 * Flushes the log; also closes it if it hasn't been used for a while.
		 *
		 * @return True if file has been closed
		 */
		synchronized boolean flush()
		{
			// Flush if required
			if (!flushed)
			{
				try
				{
					writer.flush();
					flushed = true;
				}
				catch (IOException e)
				{
					System.err.println("Error flushing logfile:");
					e.printStackTrace();
				}
			}

			// Close file if required
			if (System.currentTimeMillis() - lastWrite > LOGSTAYOPEN)
			{
				try
				{
					writer.close();
				}
				catch (IOException e)
				{
					System.err.println("Error closing logfile:");
					e.printStackTrace();
				}
				writer = null;
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	/**
	 * @param folder Folder where logs should be stored
	 * @param showLevel Minimum level of data to log
	 * @param logDays Number of days to keep logfiles for
	 * @param thisAddress IP address of current server
	 * @param thisPort Port of current server
	 */
	public Logger(File folder, Level showLevel, int logDays,
		InetAddress thisAddress, int thisPort)
	{
		this.folder = folder;
		this.showLevel = showLevel;
		this.logDays = logDays;
		// Windows doesn't like colons in filenames, so change those from IPv6
		// address if present
		address = thisAddress.getHostAddress().replace(':', '!') + "_" + thisPort;

		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				logThread();
			}
		}, "Log update thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	private void logThread()
	{
		long lastDeleteCheck = 0;
		while (true)
		{
			// Wait
			boolean closeRequired;
			synchronized (threadSynch)
			{
				try
				{
					threadSynch.wait(LOGUPDATEFREQUENCY);
				}
				catch (InterruptedException e)
				{
				}
				closeRequired = close;
			}

			// If close is requested
			if (closeRequired)
			{
				// Close all the files
				synchronized (openFiles)
				{
					for (OpenFile file : openFiles.values())
					{
						file.close();
					}
					openFiles.clear();
				}

				// Tell close method to stop waiting
				synchronized (threadSynch)
				{
					closed = true;
					threadSynch.notifyAll();
				}
				return;
			}

			// Otherwise flush any files with data and chuck away any that haven't
			// been used in a while
			synchronized (openFiles)
			{
				for (Iterator<OpenFile> i = openFiles.values().iterator(); i.hasNext();)
				{
					OpenFile file = i.next();
					if (file.flush())
					{
						i.remove();
					}
				}
			}

			// Every so often, check and delete old log files
			if (logDays != 0
				&& System.currentTimeMillis() - lastDeleteCheck > DELETECHECKFREQUENCY)
			{
				File[] files = folder.listFiles();
				if (files != null)
				{
					long threshold =
						System.currentTimeMillis() - (long)logDays * 24 * 60 * 60 * 1000;
					int deleted = 0;
					for (File f : files)
					{
						if (!f.getName().matches(".*\\.[0-9]{4}-[0-9]{2}-[0-9]{2}\\.log"))
						{
							continue;
						}

						if (f.lastModified() < threshold)
						{
							if (f.delete())
							{
								deleted++;
							}
							else
							{
								log(SYSTEMLOG, Level.ERROR, "Error deleting old log "
									+ f.getName());
							}
						}
					}
					if (deleted > 0)
					{
						log(SYSTEMLOG, Level.NORMAL, "Deleted " + deleted + " old logs");
					}
				}
			}
		}
	}

	/** Closes the logging system, terminating its thread. */
	public void close()
	{
		synchronized (threadSynch)
		{
			close = true;
			threadSynch.notifyAll();

			while (!closed)
			{
				try
				{
					threadSynch.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	/**
	 * Logs a line of text.
	 *
	 * @param fileName Base name of the file; the date and .log will be added
	 * @param level Level of logging
	 * @param line Line to log, not including terminating \n
	 */
	public void log(String fileName, Level level, String line)
	{
		if (!level.isAtLeast(showLevel))
		{
			return;
		}

		// Work out filename including date
		Date now = new Date();
		String date = (new SimpleDateFormat("yyyy-MM-dd")).format(now);
		File target = getLogFile(fileName, date);

		// Open file if necessary
		OpenFile file;
		synchronized (openFiles)
		{
			file = openFiles.get(target);
			if (file == null)
			{
				try
				{
					file =
						new OpenFile(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(target, true), "UTF-8")));
				}
				catch (IOException e)
				{
					System.err.println("Error opening logfile " + target + ":");
					e.printStackTrace();
					return;
				}
				openFiles.put(target, file);
			}
		}

		// Add time to line
		String time = (new SimpleDateFormat("HH:mm:ss")).format(now);

		// Write line
		file.write(time + " " + line);
	}

	/**
	 * Logs a line of text with an exception.
	 *
	 * @param fileName Base name of the file; the date and .log will be added
	 * @param level Level of logging
	 * @param line Line to log, not including terminating \n
	 * @param t Exception to log
	 */
	public void log(String fileName, Level level, String line, Throwable t)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		log(fileName, level, line + " [Exception]\n" + sw.toString() + "\n");
	}

	/**
	 * @param fileName Log name (channel usually)
	 * @param date Date in YYYY-MM-DD format
	 * @return True if a channel logfile for the given date exists on this server
	 */
	public boolean hasLog(String fileName, String date)
	{
		return getLogFile(fileName, date).exists();
	}

	/**
	 * @param fileName Log name (channel or SYSTEMLOG)
	 * @param date Date in YYYY-MM-DD format
	 * @return File object
	 */
	private File getLogFile(String fileName, String date)
	{
		if (fileName.equals(SYSTEMLOG))
		{
			fileName = "!system." + address;
		}
		return new File(folder, fileName + "." + date + ".log");
	}

	/**
	 * @param fileName Log name (channel usually)
	 * @param date Date in YYYY-MM-DD format
	 * @return Log expressed as a JavaScript array, one entry per log line
	 * @throws OperationException If there's any error reading the file
	 */
	public String getLogJS(String fileName, String date)
		throws OperationException
	{
		try
		{
			StringBuilder js = new StringBuilder();
			File target = getLogFile(fileName, date);
			BufferedReader br =
				new BufferedReader(new InputStreamReader(new FileInputStream(target),
					"UTF-8"));
			boolean first = true;
			while (true)
			{
				String line = br.readLine();
				if (line == null)
				{
					break;
				}

				if (first)
				{
					js.append('[');
					first = false;
				}
				else
				{
					js.append(',');
				}

				js.append('\'');
				js.append(JS.escapeJS(line));
				js.append('\'');
			}
			js.append(']');
			br.close();
			return js.toString();
		}
		catch (IOException e)
		{
			throw new OperationException(ErrorCode.OPERATION_LOGREAD,
				"Error reading log", e);
		}
	}

}
