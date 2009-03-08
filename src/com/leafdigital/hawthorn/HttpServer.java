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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.regex.*;

/** Server that accepts incoming HTTP requests and dispatches them as events. */
public final class HttpServer extends HawthornObject
{
	private final static int CONNECTIONTIMEOUT=90000,CLEANUPEVERY=30000,STATSEVERY=60000;

	private Selector selector;
	private ServerSocketChannel server;
	private final static int BACKLOG=16;
	private HashMap<SelectionKey,Connection> connections=
		new HashMap<SelectionKey,Connection>();

	private final static Pattern HTTPREQUEST=Pattern.compile(
		"GET (.+) HTTP/1\\.[01]");

	private final static Pattern SERVERAUTH=Pattern.compile(
		"\\*([0-9]{1,18})\\*([a-f0-9]{40})");

	private boolean close,closed;

	/**
	 * @param app Main app object
	 * @throws StartupException If there is a problem binding the socket
	 */
	public HttpServer(Hawthorn app) throws StartupException
	{
		super(app);
		try
		{
			selector=Selector.open();
			server=ServerSocketChannel.open();
			server.configureBlocking(false);
			server.socket().bind(new InetSocketAddress(
				getConfig().getThisServer().getAddress(),
				getConfig().getThisServer().getPort()),BACKLOG);
			server.register(selector,SelectionKey.OP_ACCEPT);
		}
		catch(IOException e)
		{
			throw new StartupException(ErrorCode.STARTUP_CANNOTBIND,
				"Failed to initialise server socket.",e);
		}

		Thread t=new Thread(new Runnable()
		{
			public void run()
			{
				serverThread();
			}
		},"Main server thread");
		t.start();
	}

	/**
	 * A single connection to the HTTP server.
	 */
	public class Connection
	{
		private final static int BUFFERSIZE=8192;
		private SelectionKey key;
		private SocketChannel channel;
		private ByteBuffer buffer;
		private long lastAction;
		private String hostAddress;

		private boolean otherServer;
		private boolean serverAuthenticated;

		private final static String CRLF="\r\n";

		/**
		 * @param key Selection key
		 */
		private Connection(SelectionKey key)
		{
			this.key=key;
			this.channel=(SocketChannel)key.channel();
			lastAction=System.currentTimeMillis();
			buffer=ByteBuffer.allocate(BUFFERSIZE);
			hostAddress=channel.socket().getInetAddress().getHostAddress();
		}

		/** Closes the connection */
		public void close()
		{
			try
			{
				channel.close();
			}
			catch(IOException e)
			{
				// Ignore exceptions when closing
			}
			key.cancel();
			synchronized(connections)
			{
				connections.remove(key);
			}
		}

		/**
		 * Sends an HTTP response on this connection and closes it. Note that all
		 * responses, even errors, use HTTP 200 OK. This is because we want the
		 * JavaScript, not browser, to handle the error.
		 * @param data Data to send (will be turned into UTF-8)
		 */
		public void send(String data)
		{
			send(200,data);
		}

		/**
		 * Sends an HTTP response on this connection and closes it.
		 * @param code HTTP code. Use 200 except for fatal errors where we
		 *   don't know which callback function to call
		 * @param data Data to send (will be turned into UTF-8)
		 * @throws IllegalArgumentException If the HTTP code isn't supported
		 */
		public void send(int code,String data) throws IllegalArgumentException
		{
			try
			{
				// Get data
				byte[] dataBytes=data.getBytes("UTF-8");

				// Get header
				StringBuilder header=new StringBuilder();

				String codeText;
				switch(code)
				{
				case 200 : codeText="OK"; break;
				case 404 : codeText="Not found"; break;
				case 500 : codeText="Internal server error"; break;
				default: throw new IllegalArgumentException("Unsupported HTTP code "+code);
				}

				header.append("HTTP/1.1 ");
				header.append(code);
				header.append(' ');
				header.append(codeText);
				header.append(CRLF);

				header.append("Connection: close");
				header.append(CRLF);

				header.append("Content-Type: application/javascript; charset=UTF-8");
				header.append(CRLF);

				header.append("Content-Length: ");
				header.append(dataBytes.length);
				header.append(CRLF);

				header.append(CRLF);
				byte[] headerBytes=header.toString().getBytes("US-ASCII");

				// Combine the two
				ByteBuffer response=ByteBuffer.allocate(dataBytes.length+headerBytes.length);
				response.put(headerBytes);
				response.put(dataBytes);
				response.flip();

				// Send data
				while(true)
				{
					try
					{
						channel.write(response);
					}
					catch(IOException e)
					{
						getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
							this+": Error writing data");
						close();
						return;
					}

					// Close connection if needed
					if(!response.hasRemaining())
					{
						close();
						return;
					}

					// Still some data? OK, wait a bit and try again (yay polling -
					// but this should probably never really happen)
					try
					{
						System.err.println("Doing the sleep thing");
						Thread.sleep(50);
					}
					catch(InterruptedException ie)
					{
					}
				}
			}
			catch(UnsupportedEncodingException e)
			{
				throw new Error("Basic encoding not supported?!",e);
			}
		}

		private void read()
		{
			if(buffer==null)
			{
				close();
				return;
			}

			int read;
			try
			{
				read=channel.read(buffer);
			}
			catch(IOException e)
			{
				// Connection got closed, or something else went wrong
				close();
				return;
			}
		  if(read==-1)
		  {
		  	close();
		  	return;
		  }
		  if(read==0)
			{
				return;
			}
		  lastAction=System.currentTimeMillis();

		  byte[] array=buffer.array();
		  int bufferPos=buffer.position();

		  // Might this be another server introducing itself?
		  if(!otherServer && bufferPos>0 && array[0]=='*')
		  {
		  	if(!getConfig().isOtherServer(channel.socket().getInetAddress()))
		  	{
					getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
						this+": Remote server connection from disallowed IP");
					close();
					return;
		  	}

		  	otherServer=true;
		  }

		  if(otherServer)
		  {
		  	handleServer(array,bufferPos);
		  }
		  else
		  {
		  	handleUser(array,bufferPos);
		  }
		}

		/**
		 * User communication follows HTTP.
		 * @param array Data buffer
		 * @param bufferPos Length of buffer that is filled
		 */
		private void handleUser(byte[] array,int bufferPos)
		{
			if(array[bufferPos-1]=='\n' &&
		  	array[bufferPos-2]=='\r' &&
		  	array[bufferPos-3]=='\n' &&
		  	array[bufferPos-4]=='\r')
		  {
		  	// Obtain GET/POST line
		  	int i;
		  	for(i=0;array[i]!='\r';i++)
				{
					;
				}
		  	try
				{
					String firstLine=new String(array,0,i,"US-ASCII");
					Matcher m=HTTPREQUEST.matcher(firstLine);
					if(!m.matches())
					{
						getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
							this+": Invalid request line: "+firstLine);
						close();
						return;
					}

					buffer=null;
					receivedRequest(m.group(1));
					return;
				}
				catch(UnsupportedEncodingException e)
				{
					throw new Error("Missing US-ASCII support",e);
				}
		  }
		  else
		  {
		  	// Not received valid request yet. If we've received the full buffer,
		  	// give up on it.
		  	if(bufferPos==BUFFERSIZE)
		  	{
					getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
						this+": Received large invalid request");
		  		close();
		  		return;
		  	}
		  }
		}

		/**
		 *
		 * @param array Data buffer
		 * @param bufferPos Length of buffer that is filled
		 */
		private void handleServer(byte[] array,int bufferPos)
		{
			int pos=0;
			while(true)
			{
				int linefeed;
				for(linefeed=pos;linefeed<bufferPos;linefeed++)
				{
					if(array[linefeed]=='\n')
					{
						break;
					}
				}
				// If there are no more lines, exit
				if(linefeed==bufferPos)
				{
					// Clean up the buffer to remove used data.
					System.arraycopy(array,pos,array,0,bufferPos-pos);
					buffer.position(bufferPos-pos);

					// Exit
					return;
				}

				// Process line [UTF-8]
				try
				{
					String line=new String(array,pos,linefeed-pos,"UTF-8");
					pos=linefeed+1;
					if(serverAuthenticated)
					{
						// Pass this to event-handler
						getEventHandler().addEvent(new ServerEvent(getApp(),line,this));
					}
					else
					{
						// This must be authentication method
						Matcher m=SERVERAUTH.matcher(line);
						if(m.matches())
						{
							// Check time. This is there both to ensure the security check
							// isn't easily reproducible - which it's a bit crap for, since
							// I didn't make sure that times aren't reused - and to ensure
							// that clocks are in synch, because if they aren't, behaviour
							// will be weird.
							long time=Long.parseLong(m.group(1));
							if(Math.abs(time-System.currentTimeMillis()) > 5000)
							{
								getLogger().log(Logger.SYSTEMLOG,Logger.Level.ERROR,
									this+": Remote server reports incorrect time (>5 seconds " +
										"out). You must use network time synchronization for all " +
										"servers.");
								close();
								return;
							}

							// Build hash using time and IP address
							String valid=getApp().getValidKey(
								"remote server",toString(),"",time+"");
							if(!valid.equals(m.group(2)))
							{
								getLogger().log(Logger.SYSTEMLOG,Logger.Level.ERROR,
									this+": Invalid remote server authorisation key: "+line);
							}

							serverAuthenticated=true;
							getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
								this+": Successful remote server login");
						}
						else
						{
							getLogger().log(Logger.SYSTEMLOG,Logger.Level.ERROR,
								this+": Invalid remote server auth line: "+line);
							close();
							return;
						}

					}
				}
				catch(UnsupportedEncodingException e)
				{
					throw new Error("Missing UTF-8 support",e);
				}
				catch(OperationException e)
				{
					throw new Error(e);
				}
			}
		}

		@Override
		/**
		 * @return Internet address (numeric) of this connection
		 */
		public String toString()
		{
			return hostAddress;
		}

		private void receivedRequest(String request)
		{
			getLogger().log(Logger.SYSTEMLOG,Logger.Level.DETAIL,
				this+": Requested "+request);
			getEventHandler().addEvent(new HttpEvent(getApp(),request,this));
		}

		private boolean checkTimeout(long now)
		{
			if(!serverAuthenticated && now-lastAction>CONNECTIONTIMEOUT)
			{
				getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
					channel.socket().getInetAddress().getHostAddress()+" (timeout)");
				close();
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	private void serverThread()
	{
		long lastCleanup=System.currentTimeMillis(),lastStats=lastCleanup;
		try
		{
			while(true)
			{
				selector.select(5000);
				if(close)
				{
					closed=true;;
					return;
				}

				for(SelectionKey key : selector.selectedKeys())
				{
					if((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT)
					{
						try
						{
							Socket newSocket=server.socket().accept();
							newSocket.getChannel().configureBlocking(false);
							SelectionKey newKey=newSocket.getChannel().register(
								selector,SelectionKey.OP_READ);
							Connection newConnection=new Connection(newKey);
							synchronized(connections)
							{
								connections.put(newKey,newConnection);
							}
						}
						catch(IOException e)
						{
							getLogger().log(Logger.SYSTEMLOG,
								Logger.Level.ERROR,"Failed to accept connection", e);
						}
					}
					if((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ)
					{
						Connection c;
						synchronized(connections)
						{
							c=connections.get(key);
							if(c==null)
							{
								continue;
							}
						}
						c.read();
					}
					if(!key.isValid())
					{
						synchronized(connections)
						{
							connections.remove(key);
						}
					}
				}
				selector.selectedKeys().clear();

				long now=System.currentTimeMillis();
				if(now-lastCleanup > CLEANUPEVERY)
				{
					lastCleanup=now;
					LinkedList<Connection> consider;
					synchronized(connections)
					{
						consider=new LinkedList<Connection>(connections.values());
					}
					for(Connection connection : consider)
					{
						connection.checkTimeout(now);
					}
				}

				if(now-lastStats > STATSEVERY)
				{
					lastStats=now;
					int count;
					synchronized(connections)
					{
						count=connections.size();
					}
					getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
						"Server stats: connection count "+count);

				}
			}
		}
		catch(Throwable t)
		{
			getLogger().log(Logger.SYSTEMLOG,
				Logger.Level.FATALERROR,"Fatal error in main server thread", t);
			// If the main thread crashed, better exit the whole server
			closed=true;
			getApp().close();
		}
	}

	/**
	 * Closes the HTTP server. Note that this will block for a little while.
	 */
	public void close()
	{
		close=true;
		while(!closed)
		{
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException ie)
			{
			}
		}
	}
}
