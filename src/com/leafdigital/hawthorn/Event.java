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

/** Event that can be handled by {@link EventHandler}. */
public abstract class Event extends HawthornObject
{
	/**
	 * @param app Application main object
	 */
	protected Event(Hawthorn app)
	{
		super(app);
	}

	/**
	 * Processses the event. This will be called on an event thread.
	 * @throws OperationException If there is an unhandled error while
	 *   processing the event
	 */
	public abstract void handle() throws OperationException;
}
