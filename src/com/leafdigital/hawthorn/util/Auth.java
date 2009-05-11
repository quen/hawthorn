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
package com.leafdigital.hawthorn.util;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.util.*;

/** Calculates Hawthorn authentication keys. */
public abstract class Auth
{
	/** Permissions */
	public enum Permission
	{
		/** Read permission */
		READ("r"),
		/** Write permission */
		WRITE("w"),
		/** Moderate permission */
		MODERATE("m"),
		/** View log (and statistics, etc) permission */
		ADMIN("a");

		private String code;
		private Permission(String code)
		{
			this.code = code;
		}

		@Override
		public String toString()
		{
			return code;
		}
	}

	/**
	 * Converts a permission string to an EnumSet.
	 * @param permissions A string of permission characters in correct order
	 * @return EnumSet of the equivalent
	 * @throws IllegalArgumentException If any character in the string is invalid
	 */
	public static EnumSet<Permission> getPermissionSet(String permissions)
		throws IllegalArgumentException
	{
		EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
		int pos = 0;
		for(Permission p : EnumSet.allOf(Permission.class))
		{
			if(pos == permissions.length())
			{
				// Happens if string is empty
				break;
			}
			if(permissions.charAt(pos) == p.toString().charAt(0))
			{
				result.add(p);
				pos++;
			}
		}
		if(pos != permissions.length())
		{
			throw new IllegalArgumentException("Invalid permissions: " + permissions);
		}
		return result;
	}

	/**
	 * Converts an EnumSet to a permission string.
	 * @param permissionSet Set of permissions
	 * @return String equivalent
	 */
	public static String getPermissions(EnumSet<Permission> permissionSet)
	{
		StringBuilder out = new StringBuilder();
		for(Permission p : permissionSet)
		{
			out.append(p.toString());
		}
		return out.toString();
	}

	/**
	 * Generates an authentication key based on SHA-1 hash.
	 * @param magicNumber Server's secret number
	 * @param user User ID
	 * @param displayName Display name
	 * @param extra Extra user data
	 * @param permissionSet Permissions
	 * @param channel Channel ID
	 * @param keyTime Time of key
	 * @return Valid key
	 * @throws NoSuchAlgorithmException If SHA-1 isn't installed
	 */
	public static String getKey(String magicNumber,
		String user, String displayName, String extra,
		EnumSet<Permission> permissionSet, String channel,
		long keyTime) throws NoSuchAlgorithmException
	{
		// Obtain data used for hash
		StringBuilder out = new StringBuilder();
		out.append(channel);
		out.append("\n");
		out.append(user);
		out.append("\n");
		out.append(displayName);
		out.append("\n");
		out.append(extra);
		out.append("\n");
		for(Permission p : permissionSet)
		{
			out.append(p.toString());
		}
		out.append("\n");
		out.append(Long.toString(keyTime));
		out.append("\n");
		out.append(magicNumber);

		return hash(out.toString());
	}

	/**
	 * Call to enable the thread-local hash cache. This must be called on each
	 * thread that uses the cache. If a thread is to be discarded, call this
	 * again to disable the cache and free space.
	 * According to my testing, a single thread cache takes about 1MB (on a 64-bit
	 * system).
	 * @param enable True to enable the cache for this thread
	 */
	public static void enableThreadCache(boolean enable)
	{
		if(enable)
		{
			fastCaches.set(new FastCache());
		}
		else
		{
			fastCaches.set(null);
		}
	}

	/**
	 * Number of entries to store in the hash cache (per thread).
	 * This value must, in binary, have all its rightmost bits set to 1 (ie
	 * after you have a 0 you can't have any 1s to the left of it).
	 */
	private final static int CACHE_MASK = 0x7fff;

	/** Thread-local variable storing cache for each thread. */
	private static ThreadLocal<FastCache> fastCaches =
		new ThreadLocal<FastCache>();

	/**
	 * Cache stores names and values according to their hashCode,
	 * bitwise AND with CACHE_MASK.
	 */
	private static class FastCache
	{
		private String[] fastCache = new String[CACHE_MASK+1];
		private String[] fastCacheValue = new String[CACHE_MASK+1];
	}

	/**
	 * @param string String to hash
	 * @return SHA-1 hash of strung
	 * @throws NoSuchAlgorithmException If Java is missing the SHA-1 provider
	 */
	public static String hash(String string) throws NoSuchAlgorithmException
	{
		// Try cache
		int hashCode = -1;
		FastCache cache = fastCaches.get();
		if(cache!=null)
		{
			hashCode = string.hashCode() & CACHE_MASK;
			if(string.equals(cache.fastCache[hashCode]))
			{
				return cache.fastCacheValue[hashCode];
			}
		}

		// Get bytes
		byte[] hashDataBytes;
		try
		{
			hashDataBytes = string.getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new Error("No UTF-8 support?!", e);
		}

		// Hash data and return 40-character string
		MessageDigest m;
		m = MessageDigest.getInstance("SHA-1");
		m.update(hashDataBytes, 0, hashDataBytes.length);
		String sha1 = new BigInteger(1, m.digest()).toString(16);
		while(sha1.length() < 40)
		{
			sha1 = "0" + sha1;
		}

		if(cache!=null)
		{
			cache.fastCache[hashCode] = string;
			cache.fastCacheValue[hashCode] = sha1;
		}
		return sha1;
	}
}
