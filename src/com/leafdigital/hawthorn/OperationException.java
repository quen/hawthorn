package com.leafdigital.hawthorn;

/** Exception thrown during the operation of the system. */
public class OperationException extends HawthornException
{
	/**
	 * @param code Unique error CODE_xx 
	 * @param message Message text
	 */
	OperationException(ErrorCode code,String message)
	{
		this(code,message,null);
	}
	
	/**
	 * @param code Unique error code
	 * @param message Message text
	 * @param cause Cause of error or null if none
	 */
	OperationException(ErrorCode code,String message,Throwable cause)
	{
		super(code,message,cause);
	}	

}
