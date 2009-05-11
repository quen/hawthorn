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

	private final static int CACHE_MASK = 0x7fff;
	private static ThreadLocal<FastCache> fastCaches =
		new ThreadLocal<FastCache>();

	/**
	 * Call to enable the thread-local hash cache. This must be called on each
	 * thread that uses the cache. If a thread is to be discarded, call this
	 * again to disable the cache and free space.
	 * @param enable True to enable the cache for this thread
	 */
	public static void enableHashCache(boolean enable)
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

	private static class FastCache
	{
		String[] fastCache = new String[CACHE_MASK+1];
		String[] fastCacheValue = new String[CACHE_MASK+1];
	}

private static int cacheHit, cacheMiss;

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
				cacheHit++;
				return cache.fastCacheValue[hashCode];
			}
			cacheMiss++;
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

	public static void main(String[] args) throws Exception
	{
		String[] lines = new String[50000];
		String[] out = new String[lines.length], out2 = new String[lines.length];
		int[] outI = new int[lines.length];
		BufferedReader in=new BufferedReader(new InputStreamReader(
			new FileInputStream(System.getProperty("user.home")+"/hash.txt")));
		for(int i=0;i<lines.length;i++)
		{
			lines[i]=in.readLine().replace("----LINEBREAK----","\n");
		}
		in.close();
		System.err.println("Read data. "+System.getProperty("java.version"));

		int loops = 1;

		for(int k = 0 ; k<2 ; k++)
		{
			enableHashCache(false);
			long start = System.currentTimeMillis();
			for(int j=0;j<loops;j++)
			{
				for(int i=0;i<lines.length;i++)
				{
					out[i] = hash(lines[i]);
				}
			}
			long end = System.currentTimeMillis();

			System.err.println();
			System.err.println("Random entry: "+out[(int)(Math.random()*lines.length)]);
			System.err.println("Random entry: "+outI[(int)(Math.random()*lines.length)]);

			System.err.println("Before Total time "+(end-start));
			System.err.println("Before Time per hash "+((double)(end-start)/(double)(lines.length*loops)));

			enableHashCache(true);
			start = System.currentTimeMillis();
			for(int j=0;j<loops;j++)
			{
				for(int i=0;i<lines.length;i++)
				{
					out2[i] = hash(lines[i]);
				}
			}
			end = System.currentTimeMillis();

			System.err.println();
			System.err.println("Random entry: "+out2[(int)(Math.random()*lines.length)]);
			System.err.println("Random entry: "+outI[(int)(Math.random()*lines.length)]);

			System.err.println("After Total time "+(end-start));
			System.err.println("After Time per hash "+((double)(end-start)/(double)(lines.length*loops)));

			System.err.println("Hit %: "+((cacheHit*100.0) / (cacheHit + cacheMiss)));
			cacheHit=0; cacheMiss =0;

			Thread.sleep(1000);
		}

		for(int i=0;i<lines.length;i++)
		{
			if(!out[i].equals(out2[i]))
			{
				System.err.println("Diffrent answers!");
				System.exit(0);
			}
		}

		for(int i=0;i<10;i++)
		{
			System.gc();
			Thread.sleep(100);
		}
		long withCache = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		enableHashCache(false);
		for(int i=0;i<10;i++)
		{
			System.gc();
			Thread.sleep(100);
		}
		long withoutCache = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		System.err.println("Cache size "+((withCache-withoutCache)/1024)+" KB");
	}
}
