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
