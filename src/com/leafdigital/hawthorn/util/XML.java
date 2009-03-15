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

import java.io.IOException;

import javax.xml.parsers.*;

import org.w3c.dom.*;

/** XML utilities. */
public abstract class XML
{
	/**
	 * Obtains a DocumentBuilder for use when parsing XML.
	 * @return DocumentBuilder
	 * @throws ParserConfigurationException If Java is screwed
	 */
	public static DocumentBuilder getDocumentBuilder()
		throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		return dbf.newDocumentBuilder();
	}

	/**
	 * Gets text from within an XML element.
	 * @param container Element that contains text
	 * @return Text as string
	 * @throws IOException If element contains anything except text
	 */
	public static String getText(Element container) throws IOException
	{
		StringBuilder result = new StringBuilder();
		NodeList children = container.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			if (!(children.item(i) instanceof Text))
			{
				throw new IOException("Element "
					+ container.getTagName() + " must not include XML tags.");
			}
			result.append(((Text)children.item(i)).getData());
		}
		return result.toString().trim();
	}
}
