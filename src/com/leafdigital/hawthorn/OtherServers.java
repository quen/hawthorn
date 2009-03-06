package com.leafdigital.hawthorn;

import java.io.*;
import java.net.*;
import java.util.LinkedList;

/** 
 * Maintains connections to the other servers and transmits 
 * directly-received say commands to them.
 */
public class OtherServers extends HawthornObject
{
	private int TRANSFERLIMIT=1000;
	
	private int RETRYDELAY=60*1000;
	private int FLUSHDELAY=300;
	
	private OtherServer[] otherServers;
	private int count;
	
	
	/**
	 * @param app Application main object
	 */
	public OtherServers(Hawthorn app)
	{
		super(app);
		otherServers=new OtherServer[app.getConfig().getOtherServers().length];
		count=0;
		for(Configuration.ServerInfo info : app.getConfig().getOtherServers())
		{
			otherServers[count++]=new OtherServer(info.getAddress(),info.getPort());						
		}		
	}
	
	/** Closes threads */
	public void close()
	{
		for(int i=0;i<otherServers.length;i++)
		{
			otherServers[i].close();
		}
	}

	/**
	 * Adds message to the send-queue for all remote servers.
	 * @param m Message
	 */
	public void sendMessage(Message m)
	{
		for(int i=0;i<otherServers.length;i++)
		{
			otherServers[i].sendMessage(m);
		}
	}
	
	/** Manages connection to another server. */
	private final class OtherServer extends Thread
	{
		private LinkedList<Message> waiting=new LinkedList<Message>();
		private InetAddress address;
		private int port;
		
		private boolean close,closed;
		
		
		/**
		 * @param address Address for connection
		 * @param port Port for connection
		 */
		private OtherServer(InetAddress address,int port)
		{
			super("Remote server thread for "+address+" port "+port);
			this.address=address;
			this.port=port;
			start();
		}
		
		synchronized void sendMessage(Message m)
		{
			// Add to transfer list
			waiting.addLast(m);
			notify();
			
			// Do not build up an infinite list, if we can't get through to the
			// remote server.
			if(waiting.size()>TRANSFERLIMIT)
				waiting.removeFirst();			
		}
		
		synchronized void close()
		{
			close=true;
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
			long lastFailure=System.currentTimeMillis();
			boolean flushed=true;
			outerloop: while(true)				
			{
				synchronized(this)
				{
					if(close)
					{
						closed=true;
						break;
					}
				}
				
				try
				{
					// Connect to server
					Socket s=new Socket(address,port);
					BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(
						s.getOutputStream(),"UTF-8"));
					getLogger().log(Logger.SYSTEMLOG,Logger.Level.NORMAL,
						this+": Connected to remote server");
					
					// Send authentication
					long now=System.currentTimeMillis();
					String hash=getApp().getValidKey(
						"remote server",address.getHostAddress(),"",now+"");
					writer.write("*"+now+"*"+hash+"\n");
					writer.flush();
					
					// Keep sending things when there's something to send
					while(true)
					{
						Message m;
						synchronized(this)
						{
							while(waiting.isEmpty())
							{
								if(!flushed)
								{
									wait(FLUSHDELAY);
									writer.flush();
									flushed=true;									
								}
								else
								{
									wait();
								}
								if(close)
								{
									closed=true;
									break outerloop;
								}
							}
							
							m=waiting.removeFirst();							
						}
						String say="SAY "+m.getChannel()+" "+m.getIP()+" "+m.getUser()+" \""+
							m.getDisplayName()+"\" "+m.getMessage();
						try
						{
							writer.write(say+"\n");						
						}
						catch(Throwable t)
						{
							// If there's an error, put back the message we failed to send
							// (this is probably a bit pointless since it's buffered so we
							// may not spot an error until later, but).
							synchronized(this)
							{
								waiting.addFirst(m);
							}
							throw t;
						}
						getLogger().log(Logger.SYSTEMLOG,Logger.Level.DETAIL,
							this+": Sent "+say);
						flushed=false;
					}
				}
				catch(Throwable t)
				{
					getLogger().log(Logger.SYSTEMLOG,Logger.Level.ERROR,
						this+": Remote server send error",t);
					long now=System.currentTimeMillis();
					if(now-lastFailure < RETRYDELAY)						
					{
						try
						{
							synchronized(this)
							{
								wait(RETRYDELAY);
							}
						}
						catch(InterruptedException e1)
						{
						}
					}
					lastFailure=now;
				}
			}
		}

		@Override
		public String toString()
		{
			return address+" port "+port;
		}
		
	}

}
