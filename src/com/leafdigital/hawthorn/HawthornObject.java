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
