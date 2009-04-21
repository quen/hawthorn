package com.leafdigital.hawthorn.loadtest;

/** Something that can generate events for the load-test threads. */
public interface EventSource
{
	/**
	 * Called when it is time to generate an event from this item.
	 * @param threadIndex Index of thread the event is happening on
	 */
	public void event(int threadIndex);
}
