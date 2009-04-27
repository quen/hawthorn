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
		for(int i = 0; i < children.getLength(); i++)
		{
			if(!(children.item(i) instanceof Text))
			{
				throw new IOException("Element "
					+ container.getTagName() + " must not include XML tags.");
			}
			result.append(((Text)children.item(i)).getData());
		}
		return result.toString().trim();
	}

	/**
	 * Escapes plain text for inclusion in XML.
	 * @param original Original text
	 * @return Escaped text
	 */
	public static String esc(String original)
	{
		return original.replace("&","&amp;").replace("<","&lt;").
			replace("'","&apos;").replace("\"","&quot;");
	}

	/**
	 * Returns a framework XHTML Strict document.
	 * @param title Contents of title tag (need not be escaped)
	 * @param headContent Head content apart from title (HTML). Null if none.
	 * @param bodyContent Body content apart from initial H1 (HTML)
	 * @return String containing full XHTML document
	 */
	public static String getXHTML(String title, String headContent,
		String bodyContent)
	{
		return
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
			"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html>\n  <head>\n    <title>" + XML.esc(title) + "</title>\n" +
			(headContent == null ? "" : headContent) + "\n</head>\n" +
			"  <body>\n    <h1>"+XML.esc(title)+"</h1>\n" + bodyContent +
			"  </body>\n</html>";
	}
}
