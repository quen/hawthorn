package com.leafdigital.hawthorn.loadtest;

/**
 * Represents one of the users who is currently active. (May change which
 * user it actually is periodically!)
 */
public class ActiveUser
{
	private LoadTest test;
	private LoadTest.SiteUser user;
	private String channel;

	private String parameters;

	private long lastTimeStamp;

	private final static int REQUEST=0, POLL=1, SAY=2, CLOSE=3;

	private long[] nextType = new long[4];

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

	public void event()
	{
		// Find the first event
		long min = Long.MAX_VALUE;
		int minType = -1;
		for (int i = 0; i < nextType.length; i++)
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
			doRequest();
			break;
		case POLL:
			doPoll();
			break;
		case SAY:
			doSay();
			break;
		case CLOSE:
			doClose();
			break;
		}

		// Put us back in the queue for the next event
		updateNextEvent();
	}

	private void doRequest()
	{
		nextType[REQUEST] = 0;
		LoadTest.TimeResult result = test.doRecent(parameters);
		nextType[POLL] = result.getNextRequest();
		lastTimeStamp = result.getTimeStamp();
	}

	private void doPoll()
	{
		LoadTest.TimeResult result = test.doPoll(parameters, lastTimeStamp);
		nextType[POLL] = result.getNextRequest();
		lastTimeStamp = result.getTimeStamp();
	}

	private void doSay()
	{
		nextType[SAY] = System.currentTimeMillis() + test.pickSayPause();
		test.doSay(parameters);
	}

	private void doClose()
	{
		nextType[CLOSE] = System.currentTimeMillis() + test.pickSessionLength();
		if (test.pickSendLeave())
		{
			test.doLeave(parameters);
		}
		newUser();
	}

	private void updateNextEvent()
	{
		long min = Long.MAX_VALUE;
		for (int i = 0; i < nextType.length; i++)
		{
			if(nextType[i] != 0 && nextType[i] < min)
			{
				min = nextType[i];
			}
		}
		test.queueUser(this, min);
	}
}
