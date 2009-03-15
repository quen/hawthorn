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
package com.leafdigital.hawthorn.auth;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;

/** Calculates Hawthorn authentication keys. */
public abstract class Auth
{
	/**
	 * Generates an authentication key based on SHA-1 hash.
	 * @param magicNumber Server's secret number
	 * @param user User ID
	 * @param displayName Display name
	 * @param channel Channel ID
	 * @param keyTime Time of key
	 * @return Valid key
	 * @throws NoSuchAlgorithmException If SHA-1 isn't installed
	 */
	public static String getKey(String magicNumber,String user,String displayName,
		String channel,long keyTime) throws NoSuchAlgorithmException
	{
		// Obtain data used for hash
		String hashData =
			channel + "\n" + user + "\n" + displayName + "\n" + keyTime + "\n"
				+ magicNumber;
		byte[] hashDataBytes;
		try
		{
			hashDataBytes = hashData.getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new Error("No UTF-8 support?!", e);
		}

		// Hash data and return 40-character string
		MessageDigest m;
		m = MessageDigest.getInstance("SHA-1");
		m.update(hashDataBytes, 0, hashDataBytes.length);
		String sha1 = new BigInteger(1, m.digest()).toString(16);
		while (sha1.length() < 40)
		{
			sha1 = "0" + sha1;
		}

		return sha1;
	}
}
