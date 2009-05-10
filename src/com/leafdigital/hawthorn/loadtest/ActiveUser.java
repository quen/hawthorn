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

/**
 * Represents one of the users who is currently active. (May change which
 * user it actually is periodically!)
 */
public class ActiveUser implements EventSource
{
	private LoadTest test;
	private LoadTest.SiteUser user;
	private String channel;

	private String parameters;

	private long lastTimeStamp;

	private final static int REQUEST=0, POLL=1, SAY=2, CLOSE=3;

	private long[] nextType = new long[4];

	private int unique = 0;

	/**
	 * @param test Load test
	 */
	public ActiveUser(LoadTest test)
	{
		this.test = test;
		newUser();
		updateNextEvent();
	}

	private void newUser()
	{
		this.user = test.pickSiteUser();
		this.channel = test.pickChannel();
		parameters = user.getParameters(channel);

		long now = System.currentTimeMillis();
		nextType[REQUEST] = now;
		nextType[POLL] = 0;
		nextType[SAY] = now + test.pickSayPause();
		nextType[CLOSE] = now + test.pickSessionLength();
	}

	public void event(int threadIndex)
	{
		// Find the first event
		long min = Long.MAX_VALUE;
		int minType = -1;
		for(int i = 0; i < nextType.length; i++)
		{
			if(nextType[i] != 0 && nextType[i] < min)
			{
				min = nextType[i];
				minType = i;
			}
		}

		// Do that event
		switch(minType)
		{
		case REQUEST:
			doRequest(threadIndex);
			break;
		case POLL:
			doPoll(threadIndex);
			break;
		case SAY:
			doSay(threadIndex);
			break;
		case CLOSE:
			doClose(threadIndex);
			break;
		}

		// Put us back in the queue for the next event
		updateNextEvent();
	}

	private void doRequest(int threadIndex)
	{
		nextType[REQUEST] = 0;
		LoadTest.TimeResult result = test.doRecent(parameters, threadIndex);
		nextType[POLL] = result.getNextRequest();
		lastTimeStamp = result.getTimeStamp();
	}

	private void doPoll(int threadIndex)
	{
		LoadTest.TimeResult result = test.doPoll(parameters, lastTimeStamp,
			threadIndex);
		nextType[POLL] = result.getNextRequest();
		lastTimeStamp = result.getTimeStamp();
	}

	private void doSay(int threadIndex)
	{
		nextType[SAY] = System.currentTimeMillis() + test.pickSayPause();
		test.doSay(parameters, threadIndex, unique++);
	}

	private void doClose(int threadIndex)
	{
		nextType[CLOSE] = System.currentTimeMillis() + test.pickSessionLength();
		if(test.pickSendLeave())
		{
			test.doLeave(parameters, threadIndex);
		}
		newUser();
	}

	private void updateNextEvent()
	{
		long min = Long.MAX_VALUE;
		for(int i = 0; i < nextType.length; i++)
		{
			if(nextType[i] != 0 && nextType[i] < min)
			{
				min = nextType[i];
			}
		}
		test.queueUser(this, min);
	}
}
