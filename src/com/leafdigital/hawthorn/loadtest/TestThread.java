package com.leafdigital.hawthorn.loadtest;

/** Test thread. */
public final class TestThread extends Thread
{
	private LoadTest test;
	private int index;

	/**
	 * @param test Main class
	 */
	public TestThread(LoadTest test, int index)
	{
		super("Load test thread");
		this.test = test;
		this.index = index;
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
			user.event(index);
		}
	}
}
