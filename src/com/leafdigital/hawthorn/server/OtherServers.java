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
import java.util.LinkedList;

import com.leafdigital.hawthorn.util.Auth;

/**
 * Maintains connections to the other servers and transmits directly-received
 * say commands to them.
 */
public class OtherServers extends HawthornObject
{
	/** Number of buffered messages while other server is down. */
	private final static int TRANSFER_LIMIT = 1000;
	/** Delay after failing to connect to remote server. */
	private final static int RETRY_DELAY = 30 * 1000;
	/** Delay before flushing send buffer. */
	private final static int FLUSH_DELAY = 300;

	private OtherServer[] otherServers;

	private int count;

	/**
	 * @param app Application main object
	 * @throws StartupException If initialisation fails
	 */
	public OtherServers(Hawthorn app) throws StartupException
	{
		super(app);
		Message.initMessageTypes();
		otherServers = new OtherServer[app.getConfig().getOtherServers().length];
		count = 0;
		for(Configuration.ServerInfo info : app.getConfig().getOtherServers())
		{
			otherServers[count++] =
				new OtherServer(info.getAddress(), info.getPort());
		}
	}

	/** Closes threads */
	public void close()
	{
		for(int i = 0; i < otherServers.length; i++)
		{
			otherServers[i].close();
		}
	}

	/**
	 * Adds message to the send-queue for all remote servers.
	 *
	 * @param m Message
	 */
	public void sendMessage(Message m)
	{
		for(int i = 0; i < otherServers.length; i++)
		{
			otherServers[i].sendMessage(m);
		}
	}

	/** Manages connection to another server. */
	private final class OtherServer extends Thread
	{
		private LinkedList<Message> waiting = new LinkedList<Message>();

		private InetAddress address;

		private int port;

		private boolean close, closed;


		/**
		 * @param address Address for connection
		 * @param port Port for connection
		 */
		private OtherServer(InetAddress address, int port)
		{
			super("Remote server thread for " + address + " port " + port);
			this.address = address;
			this.port = port;
			start();
		}

		synchronized void sendMessage(Message m)
		{
			// Add to transfer list
			waiting.addLast(m);
			notify();

			// Do not build up an infinite list, if we can't get through to the
			// remote server.
			if(waiting.size() > TRANSFER_LIMIT)
			{
				waiting.removeFirst();
			}
		}

		synchronized void close()
		{
			close = true;
			notify();
			while(!closed)
			{
				try
				{
					wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}

		@Override
		public void run()
		{
			boolean flushed = true;
			Socket s = null;
			outerloop: while(true)
			{
				synchronized (this)
				{
					if(close)
					{
						closed = true;
						break;
					}
				}

				try
				{
					// Connect to server
					s = new Socket(address, port);
					BufferedWriter writer =
						new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),
							"UTF-8"));
					getLogger().log(Logger.SYSTEM_LOG, Logger.Level.NORMAL, "SERVERTO "
						+ this + " CONNECTED");

					// Send authentication
					long now = System.currentTimeMillis();
					String hash = getApp().getValidKey("remote server",
						address.getHostAddress(), "", "", Auth.getPermissionSet(""), now);
					writer.write("*" + now + "*" + hash + "\n");
					writer.flush();

					// Keep sending things when there's something to send
					while(true)
					{
						Message m;
						synchronized (this)
						{
							while(waiting.isEmpty())
							{
								if(!flushed)
								{
									wait(FLUSH_DELAY);
									writer.flush();
									flushed = true;
								}
								else
								{
									wait();
								}
								if(close)
								{
									closed = true;
									break outerloop;
								}
							}

							m = waiting.removeFirst();
						}
						String say = m.getServerFormat();
						try
						{
							writer.write(say + "\n");
						}
						catch(Throwable t)
						{
							// If there's an error, put back the message we failed to send
							// (this is probably a bit pointless since it's buffered so we
							// may not spot an error until later, but).
							synchronized (this)
							{
								waiting.addFirst(m);
							}
							throw t;
						}
						getLogger().log(Logger.SYSTEM_LOG, Logger.Level.DETAIL, "SERVERTO "
							+	this + " REQUEST " + say);
						flushed = false;
					}
				}
				catch(Throwable t)
				{
					// Close socket if still open
					boolean hadConnection = s != null;
					if(hadConnection)
					{
						try
						{
							s.close();
							s = null;
						}
						catch(Throwable t2)
						{
							// Ignore close failures
						}
					}

					// Log error
					if(t instanceof java.net.ConnectException)
					{
						getLogger().log(Logger.SYSTEM_LOG, Logger.Level.ERROR, "SERVERTO "
							+ this + " ERROR Connect failure: " + t.getMessage());
					}
					else if(t.getClass().equals(java.net.SocketException.class))
					{
						getLogger().log(Logger.SYSTEM_LOG, Logger.Level.ERROR, "SERVERTO "
							+ this + " ERROR Send error: " + t.getMessage());
					}
					else
					{
						getLogger().log(Logger.SYSTEM_LOG, Logger.Level.ERROR, "SERVERTO "
							+ this + " ERROR Exception", t);
					}

					// If a connect attempt failed, don't try again for a bit
					if(!hadConnection)
					{
						long until = System.currentTimeMillis() + RETRY_DELAY;
						while(System.currentTimeMillis() < until && !close)
						{
							try
							{
								synchronized (this)
								{
									wait(RETRY_DELAY);
								}
							}
							catch(InterruptedException e1)
							{
							}
						}
					}
				}
			}
		}

		@Override
		public String toString()
		{
			return address.getHostAddress() + ":" + port;
		}

	}

}
