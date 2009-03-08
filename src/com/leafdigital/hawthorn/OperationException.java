/*
Copyright 2009 Samuel Marshall
http://www.leafdigital.com/software/hawthorn/

This file is part of hawthorn.

hawthorn is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

hawthorn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with hawthorn.  If not, see <http://www.gnu.org/licenses/>.
*/
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
