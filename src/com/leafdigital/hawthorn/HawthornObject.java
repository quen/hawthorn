package com.leafdigital.hawthorn;

/** For objects that need a reference to the Hawthorn application. */
public abstract class HawthornObject
{
	private Hawthorn app;
	
	protected HawthornObject(Hawthorn app)
	{
		this.app=app;
	}
	
	/** @return App configuration */
	public Configuration getConfig()
	{
		return app.getConfig();
	}

	/** @return App logger */
	public Logger getLogger()
	{
		return getConfig().getLogger();
	}
	
	/** @return App event handler */
	public EventHandler getEventHandler()
	{
		return app.getEventHandler();
	}
	
	/** @return App object */
	public Hawthorn getApp()
	{
		return app;
	}
	
	/** @return Channels object */
	public Channels getChannels()
	{
		return app.getChannels();
	}
}
