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
