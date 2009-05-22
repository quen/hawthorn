package com.leafdigital.hawthorn.util;

import java.io.*;
import java.util.regex.*;

/** Class that provides information about the system version. */
public abstract class HawthornVersion
{
	private static String version, buildDate;

	/** Loads data from the version file */
	private static void init()
	{
		if(version == null)
		{
			try
			{
				InputStream stream = HawthornVersion.class.getResourceAsStream(
					"version.txt");
				// The file may not exist (e.g. a development build)
				if(stream!=null)
				{
					BufferedReader reader = new BufferedReader(new InputStreamReader(
						stream, "UTF-8"));
					String line = reader.readLine();
					Matcher m = Pattern.compile("^(.*?)/([^/]*)$").matcher(line);
					if(m.matches())
					{
						version = m.group(1);
						buildDate = m.group(2);
					}
					reader.close();
				}
			}
			catch(IOException e)
			{
				// Ignore errors, it's not that important
			}

			if(version==null)
			{
				version = "";
				buildDate = "unknown";
			}

		}
	}

	/**
	 * @return True if this is a release version of Hawthorn.
	 */
	public static boolean isReleaseVersion()
	{
		init();
		return version.length() > 0;
	}

	/**
	 * @return Build description string; this is either the version number
	 *   followed by date in brackets, or "DEVELOPMENT (date)"
	 */
	public static String getDescription()
	{
		if(isReleaseVersion())
		{
			return getVersion() + " (" + getBuildDate() + ")";
		}
		else
		{
			return "UNRELEASED (" + getBuildDate() + ")";
		}
	}

	/**
	 * @return Version string e.g. "1.0.1". May be empty string if not a release
	 *   version.
	 */
	public static String getVersion()
	{
		init();
		return version;
	}

	/**
	 * @return Build string e.g. "2009-05-17 13:31" or "unknown"
	 */
	public static String getBuildDate()
	{
		init();
		return buildDate;
	}

}
