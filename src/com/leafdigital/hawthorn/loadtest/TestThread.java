package com.leafdigital.hawthorn.loadtest;

/** Test thread. */
public final class TestThread extends Thread
{
	private LoadTest test;

	/**
	 * @param test Main class
	 */
	public TestThread(LoadTest test)
	{
		super("Load test thread");
		this.test = test;
		start();
	}

	@Override
	public void run()
	{
		while(true)
		{
			ActiveUser user = test.getNextEvent();
			if(user == null)
			{
				return;
			}
			user.event();
		}
	}
}
