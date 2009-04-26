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
package com.leafdigital.hawthorn.loadtest;

/** Source that generates DriveBy events for the system. */
public class DriveBySource implements EventSource
{
	private LoadTest test;
	private long next;
	private int micros;

	/**
	 * @param test Load test main class
	 */
	public DriveBySource(LoadTest test)
	{
		this.test = test;
		next = System.currentTimeMillis();
		micros = 0;
		addNextEvent();
	}

	private synchronized void addNextEvent()
	{
		test.queueUser(this, next);

		micros += test.pickDriveByDelay();
		int millis = micros / 1000;
		micros -= millis * 1000;
		next += millis;
	}

	public void event(int threadIndex)
	{
		// Note: This is not synchronized so that two drivebys can happen at
		// once (although they won't be able to add events until each finishes).
		LoadTest.SiteUser user = test.pickSiteUser();
		String channel = test.pickChannel();
		String parameters = user.getParameters(channel);

		test.doRecent(parameters, threadIndex);

		addNextEvent();
	}

}
