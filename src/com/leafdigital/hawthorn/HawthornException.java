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
	HawthornException(ErrorCode code, String message, Throwable cause)
	{
		super(code + ": " + message, cause);
		this.code = code;
		this.message = message;
	}

	@Override
	public String toString()
	{
		String result = code + ": " + message;
		if (getCause() != null)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			getCause().printStackTrace(pw);
			result += "\n\n" + sw.toString();
		}
		return result;
	}


}
