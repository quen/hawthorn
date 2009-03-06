package com.leafdigital.hawthorn;

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
	/** Error with Java security subsystem */
	OPERATION_MISSINGSHA1,
	/** Error reading logfile */
	OPERATION_LOGREAD
}
