package com.leafdigital.hawthorn;

/** Exception thrown during system startup. */
public class StartupException extends HawthornException
{
	/**
	 * @param code Unique error CODE_xx 
	 * @param message Message text
	 */
	StartupException(ErrorCode code,String message)
	{
		this(code,message,null);
	}
	
	/**
	 * @param code Unique error code
	 * @param message Message text
	 * @param cause Cause of error or null if none
	 */
	StartupException(ErrorCode code,String message,Throwable cause)
	{
		super(code,message,cause);
	}	
}
