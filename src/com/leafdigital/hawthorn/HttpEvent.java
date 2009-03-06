package com.leafdigital.hawthorn;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/** Event based on HTTP request. */
public class HttpEvent extends Event
{
	/**
	 * 
	 */
	private static final String REGEXP_LONG="[0-9]{1,18}";

	/**
	 * 
	 */
	private static final String REGEXP_INT="[0-9]{1,9}";

	private final static int KEYEXPIRY=60*60*1000;
	
	private String request;
	private HttpServer.Connection connection;
	
	/**
	 * @param app Main app object
	 * @param request HTTP request path
	 * @param connection Connection object that made this request
	 */
	HttpEvent(Hawthorn app,String request,HttpServer.Connection connection)
	{
		super(app);
		this.request=request;
		this.connection=connection;
	}

	@Override
	public void handle() throws OperationException
	{
		try
		{
			String path;
			HashMap<String,String> params=new HashMap<String,String>();
			
			int question=request.indexOf('?');
			if(question==-1)
			{
				path=request;
			}
			else
			{
				path=request.substring(0,question);
				String remainder=request.substring(question+1);
				while(remainder.length()>0)
				{
					int and=remainder.indexOf('&');
					String paramPair;
					if(and==-1)
					{
						paramPair=remainder;
						remainder="";
					}
					else
					{
						paramPair=remainder.substring(0,and);
						remainder=remainder.substring(and+1);
					}
					
					int equals=paramPair.indexOf('=');
					if(equals==-1)
					{
						params.put(paramPair,null);
					}
					else
					{
						try
						{
							params.put(paramPair.substring(0,equals),
								URLDecoder.decode(paramPair.substring(equals+1),"UTF-8"));
						}
						catch(UnsupportedEncodingException e)
						{
							throw new Error("UTF-8 not supported?!",e);
						}
					}				
				}
			}
			
			if(path.equals("/hawthorn/say"))
				handleSay(params);
			else if(path.equals("/hawthorn/getRecent"))
				handleGetRecent(params);
			else if(path.equals("/hawthorn/waitForMessage"))
				handleWaitForMessage(params);
			else if(path.equals("/hawthorn/getLog"))
				handleGetLog(params);
			else		
				handle404();		
		}
		catch(Throwable t)
		{
			connection.send(500,"// Internal server error: "+t);
			getLogger().log(Logger.SYSTEMLOG,Logger.Level.ERROR,
				"HTTP event error ("+Thread.currentThread().getName()+")",t);
			
		}
	}
	
	private void handle404()
	{
		connection.send(404,"// Unknown request address:\n// "+request);
	}
	
	private void handleSay(HashMap<String,String> params) throws OperationException
	{
		if(!checkAuth(params,"sayError", false)) return;
		String channel=params.get("channel");
		Channel c=getChannels().get(channel);
		
		String id=getID(params);
		
		String message=params.get("message");
		// Don't allow control characters
		if(message==null || !message.matches(Hawthorn.REGEXP_MESSAGE))
		{
			connection.send("sayEror("+id+",'Missing or invalid message=');");
			return;
		}
		
		Message m=new Message(System.currentTimeMillis(),channel,connection.toString(),
			params.get("user"),params.get("displayname"),message);
		getApp().getOtherServers().sendMessage(m);
		c.say(m);
		connection.send("hawthorn.sayComplete("+id+");");
	}

	/**
	 * Gets optional ID (default 0) from a request.
	 * @param params Param map
	 * @return ID string
	 */
	private String getID(HashMap<String,String> params)
	{
		String id=params.get("id");
		if(id==null || !id.matches("[0-9]{1,9}"))
		{
			id="0";
		}
		return id;
	}
	
	private void handleGetRecent(HashMap<String,String> params) throws OperationException
	{
		if(!checkAuth(params,"getRecentError", false)) return;
		Channel c=getChannels().get(params.get("channel"));
		
		String id=getID(params);
		
		String maxAge=params.get("maxage"),maxNumber=params.get("maxnumber");
		String error=null;
		if(maxAge==null || !maxAge.matches(REGEXP_INT))
		{
			error="Missing or invalid maxage=";
		}
		else if(maxNumber==null || !maxNumber.matches(REGEXP_INT))
		{
			error="Missing or invalid maxnumber=";
		}
		if(error!=null)
		{
			connection.send("hawthorn.getRecentError("+id+",'"+Hawthorn.escapeJS(error)+"');");
			return;
		}
		
		Message[] recent=c.getRecent(Integer.parseInt(maxAge),Integer.parseInt(maxNumber));
		StringBuilder output=new StringBuilder();
		output.append("hawthorn.getRecentComplete("+id+",[");
		for(int i=0;i<recent.length;i++)
		{
			if(i!=0) output.append(',');
			output.append(recent[i].getJS());
		}
		output.append("]);");
    connection.send(output.toString());
	}
	
	private void handleWaitForMessage(HashMap<String,String> params) throws OperationException
	{
		if(!checkAuth(params,"waitForMessageError", false)) return;
		Channel c=getChannels().get(params.get("channel"));
		
		String id=getID(params);

		String lastTimeString=params.get("lasttime");
		long lastTime=Channel.ANY;
		String error=null;
		int maxAge=Channel.ANY,maxNumber=Channel.ANY;
		if(lastTimeString==null)
		{
			// Instead of specifying lastTime, you can give a max age and number of
			// messages; this will retrieve existing messages, if any, that match
			// those constraints, otherwise it will wait for new ones.
			String maxAgeString=params.get("maxage"),maxNumberString=params.get("maxnumber");
			if(maxAgeString==null || !maxAgeString.matches(REGEXP_INT))
			{
				error="Missing or invalid maxage=";
			}
			else if(maxNumberString==null || !maxNumberString.matches(REGEXP_INT))
			{
				error="Missing or invalid maxnumber=";
			}
			else
			{
				maxAge=Integer.parseInt(maxAgeString);
				maxNumber=Integer.parseInt(maxNumberString);
			}
		}
		else if(!lastTimeString.matches(REGEXP_LONG))
		{
			error="hawthorn.waitForMessageError("+id+",'Invalid lasttime=');";
		}
		else
		{
			lastTime=Long.parseLong(lastTimeString);			
		}
		if(error!=null)
		{
			connection.send("hawthorn.waitForMessageError("+id+",'"+Hawthorn.escapeJS(error)+"');");
			return;
		}
		
		c.waitForMessage(connection,id,lastTime,maxAge,maxNumber);
	}
	
	private void handleGetLog(HashMap<String,String> params) throws OperationException
	{
		if(!checkAuth(params,"getLogError", true)) return;
		String channel=params.get("channel");
		
		// To retrieve log history, must use special user account
		String date=params.get("date");
		String error=null;		
		if(!params.get("user").equals("_admin"))
		{
			error="Must set user=_admin to retrieve logs";
		}
		else if(!params.get("displayname").equals("_"))
		{
			error="Must set displayname=_ to retrieve logs";
		}
		else if(date==null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
		{
			error="Must set date=YYYY-MM-DD";
		}
		else if(!getLogger().hasLog(channel,date))
		{
			error="Logs not available on this server for specified date";
		}
		if(error!=null)
		{
			connection.send("hawthorn.getLogError("+getID(params)+",'"+Hawthorn.escapeJS(error)+"');");
			return;
		}
		
		connection.send("hawthorn.getLogComplete("+getID(params)+","+
			getLogger().getLogJS(channel,date)+");");		
	}
	
	/**
	 * Check authentication for a request.
	 * @param params HTTP parameters
	 * @param errorFunction Error function to call if params are wrong
	 * @param allowSystemChannel If true, allows channel= to include !system
	 * @return True if auth is ok, false if should return (error has already been
	 *   sent)
	 * @throws OperationException Problem with hashing function
	 */
	private boolean checkAuth(HashMap<String,String> params,String errorFunction, boolean allowSystemChannel) throws OperationException
	{
		String 
			channel=params.get("channel"),
			user=params.get("user"),
			displayname=params.get("displayname"),
			key=params.get("key"),
			keytime=params.get("keytime");
		
		String error=null;
		if(channel==null || (!channel.matches(Hawthorn.REGEXP_USERCHANNEL) &&
				!(allowSystemChannel && channel.equals(Logger.SYSTEMLOG))))
		{
			error="Missing or invalid channel=";
		}
		else if(user==null || !user.matches(Hawthorn.REGEXP_USERCHANNEL))
		{
			error="Missing or invalid user=";
		}
		else if(displayname==null || !displayname.matches(Hawthorn.REGEXP_DISPLAYNAME))
		{
			// Displayname can't contain control characters or "
			error="Missing or invalid displayname=";
		}
		else if(key==null)
		{
			error="Missing key=";
		}
		else if(keytime==null || !keytime.matches(REGEXP_LONG))
		{
			error="Missing or invalid keytime=";
		}
		else if(Long.parseLong(keytime)+KEYEXPIRY < System.currentTimeMillis())
		{
			error="Expired key";
		}
		else if(!key.equals(getApp().getValidKey(channel,user,displayname,keytime)))
		{
			error="Invalid key";			
		}
		
		if(error!=null)
		{
			connection.send("hawthorn."+errorFunction+"("+getID(params)+",'"+Hawthorn.escapeJS(error)+"');");
			return false;
		}
		
		return true;
	}


}
