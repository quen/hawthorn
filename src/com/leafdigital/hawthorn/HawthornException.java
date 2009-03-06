package com.leafdigital.hawthorn;

import java.io.*;

/** Base class for Hawthorn system exceptions. */
public abstract class HawthornException extends Exception
{
	private ErrorCode code;
	private String message;

	/**
	 * @param code Unique error code
	 * @param message Message text
	 * @param cause Cause of error or null if none
	 */
	HawthornException(ErrorCode code,String message,Throwable cause)
	{
		super(code+": "+message,cause);
		this.code=code;
		this.message=message;
	}

	@Override
	public String toString()
	{
		String result=code+": "+message;
		if(getCause()!=null)
		{
			StringWriter sw=new StringWriter();
			PrintWriter pw=new PrintWriter(sw);
			getCause().printStackTrace(pw);
			result+="\n\n"+sw.toString();
		}
		return result;
	}


}
