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
package com.leafdigital.hawthorn.server;

/** Available error codes. */
public enum ErrorCode
{
	/** Incorrect command line */
	STARTUP_COMMANDLINE,
	/** Error with Java XML subsystem */
	STARTUP_XMLSYSTEM,
	/** Config file has incorrect format */
	STARTUP_CONFIGFORMAT,
	/** Error reading config file */
	STARTUP_CONFIGREAD,
	/** Log folder not writable */
	STARTUP_LOGNOTWRITEABLE,
	/** Internet address in config file not valid */
	STARTUP_INVALIDADDRESS,
	/** Error with Java security subsystem */
	STARTUP_MISSINGSHA1,
	/** Error generating test keys */
	STARTUP_TESTKEY,
	/** Not able to bind to specified address */
	STARTUP_CANNOTBIND,
	/** Unable to initialise the message constructors (reflection glitch) */
	STARTUP_MESSAGEINIT,
	/** Error reading logfile */
	OPERATION_LOGREAD,
	/** Error reading favicon */
	OPERATION_FAVICONREAD
}
