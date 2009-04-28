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

/** JavaScript-related utilities. */
public abstract class JS
{
	/**
	 * Escapes a string suitable for inclusion within JS single quotes,
	 * in a JS (not HTML) file.
	 * @param text String to escape
	 * @return String with some characters escaped
	 */
	public static String esc(String text)
	{
		return text.replace("\\", "\\\\").replace("'", "\\'");
	}

	/**
	 * Escapes a string suitable for inclusion within JS single quotes,
	 * inside a double-quoted HTML attribute.
	 * @param text String to escape
	 * @return String with some characters escaped or turned into function calls
	 */
	public static String escInlineAttr(String text)
	{
		return esc(text).replace("\"","'+String.fromCharCode(34)+'").replace(
			"&","'+String.fromCharCode(38)+'");
	}
}
