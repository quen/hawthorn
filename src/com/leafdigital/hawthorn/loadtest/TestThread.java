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

/** Test thread. */
public final class TestThread extends Thread
{
	private LoadTest test;
	private int index;

	/**
	 * @param test Main class
	 * @param index Index of thread
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
			EventSource user = test.getNextEvent();
			if(user == null)
			{
				return;
			}
			user.event(index);
		}
	}
}
