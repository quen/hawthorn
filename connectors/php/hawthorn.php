<?php
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

/**
 * Connector class for integration with the Hawthorn chat system.
 * Please see Hawthorn system documentation for detailed usage instructions
 * and examples.
 */
class Hawthorn
{
	private $magicNumber, $servers, $user, $displayName,
		$jsUrl, $popupUrl, $reAcquireUrl, $defer, $keyExpiry;

	private $recentCount, $linkToChatCount, $printedJS;

	/**
	 * Initialises Hawthorn settings.
	 * @param string $magicNumber Hawthorn authorisation magic number
	 * @param array $servers Array of server URLs (full URL ending in /)
	 * @param string $user Hawthorn ID of current user
	 * @param string $displayName Display name of current user
	 * @param string $jsUrl URL to hawthorn.js (may be relative to page)
	 * @param string $popupUrl URL to popup.html (may be relative to page)
	 * @param string $reAcquireUrl URL that re-acquires a Hawthorn key for
	 *  the user. This URL will	be called with additional parameters
	 *  channel, user, displayName, and id (the latter is used in
	 *  constructing the JavaScript response). It must check that the
	 *  user is still authenticated and then output JavaScript response
	 *  using the reAcquireAllow method.
	 * @param bool $defer If true, delays JavaScript inclusion; you must
	 *  use the includeJavaScript method before the page end
	 *  (otherwise, JavaScript is included the first time it is needed)
	 * @param int $keyExpiry How long before granted keys expire (default
	 *  1 hour), in milliseconds
	 * @throws Exception If user ID is invalid
	 */
	function __construct($magicNumber, $servers,
		$user, $displayName, $jsUrl, $popupUrl, $reAcquireUrl,
		$defer=false, $keyExpiry=3600000)
	{
		// Check username
		if (!preg_match('~^[A-Za-z0-9_]+$~',$user))
		{
			throw new Exception("Invalid user ID: $user");
		}

		$this->magicNumber = $magicNumber;
		$this->servers = $server;
		$this->user = $user;
		$this->displayName = $displayName;
		$this->jsUrl = $jsUrl;
		$this->popupUrl = $popupUrl;
		$this->reAcquireUrl = $reAcquireUrl;
		$this->defer = $defer;
		$this->keyExpiry = $keyExpiry;

		$this->recentCount = 0;
		$this->linkToChatCount = 0;
		$this->printedJS = false;
	}

	/**
	 * Displays recent messages and names from a Hawthorn chat channel.
	 * (Gives user a key granting Hawthorn access for one hour.)
	 * @param string $channel Hawthorn channel ID
	 * @param int $maxMessages Maximum number of messages to show
	 * @param int $maxAge Maximum age (in milliseconds) of messages to show
	 * @param int $maxNames Maximum number of user names to show
	 * @param string $loadingText Text to display while message information
	 *  is being loaded; if omitted, English default '(Loading chat
	 *  information, please wait...)' is used
	 * @param string $noScriptText Text to display if JavaScript is not
	 *  available; if omitted, English default '(Chat features are not
	 *  available because JavaScript is disabled.)' is used
	 * @throws Exception If channel ID is invalid
	 */
	function recent($channel, $maxMessages=3, $maxAge=900000,
		$maxNames=5, $loadingText='', $noScriptText='')
	{
		// Get index of this recent block within page
		$index = $recentCount++;

		// Get text
		if (!$loadingText)
		{
			$loadingText = '(Loading chat information, please wait...)';
		}
		if (!$noScriptText)
		{
			$noScriptText = '(Chat features are not available because '.
				'JavaScript is disabled.)';
		}

		// Output div
		print "<div id='hawthorn_recent$index' class='hawthorn_recent' ".
			"style='display:none'>$loadingText</div>\n";

		// Output no-script text
		print "<noscript>$noScriptText</noscript>\n";

		// Work out JavaScript
		$keyTime = $this->getKeyTime();
		$js = "{user:'{$this->user}', displayName:'".
			self::escapeJS($this->displayName). "',channel:'$channel',".
			"maxMessages:$maxMessages,maxAge:$maxAge,maxNames:$maxNames,".
			"key:" . $this->getKey($channel, $keyTime) .
			",id:'hawthorn_recent$index'}";

		// Print script tag if included
		$this->printJS();

		// Print the per-instance script
		print "<script type='text/javascript'>\n" .
			"/* <![CDATA[ */\n";
		print "document.getElementById('hawthorn_recent$index')." .
			"style.display = 'block';\n";
		if ($this->defer)
		{
			if ($index==0)
			{
				print "var hawthorn_recent = new Array();\n";
			}
			print "hawthorn_recent.push($js);\n";
		}
		else
		{
			print "hawthorn.handle_recent($js);\n";
		}
		print "/* ]]> */\n</script>\n";
	}

	/** Includes the Hawthorn JavaScript if it was deferred. */
	function includeJavaScript()
	{
		// If not deferred, do nothing
		if (!$this->defer)
		{
			return;
		}

		// If no relevant tags require JS, do nothing
		if (!$this->recentCount && !$this->linkToChatCount)
		{
			return;
		}

		// Print script tag
		$this->printJS(true);

		// Run all the deferred recent tags
		if ($this->recentCount)
		{
			print "<script type='text/javascript'>\n";
			print "for(var i=0;i<hawthorn_recent.length;i++)\n" .
				"{\n" +
				"\thawthorn.handleRecent(hawthorn_recent[i]);\n" +
				"}\n";
			print "</script>\n";
		}
	}

	/**
	 * Provides a link to a popup window for use to chat in a
	 * Hawthorn chat channel.
	 * (Gives user a key granting Hawthorn access for one hour.)
	 * @param string $channel Hawthorn channel ID
	 * @param string $title Popup window title
	 * @param string $linkText Text (may include HTML) to include
	 *  within link tag.
	 * @param string $icon URL to optional icon that appears beside
	 *  link to indicate that it opens in a new window
	 * @param string $iconAlt Alt text for optional icon; if omitted,
	 *  uses English-language default 'Opens in new window'.
	 * @throws Exception If channel ID is invalid
	 */
	function linkToChat($channel, $title, $linkText, $icon='',
		$iconAlt='')
	{
		// Get index of this tag within page
		$index = $this->linkToChatCount++;

		// Get auth code
		$keyTime = $this->getKeyTime();
		$key = $this->getKey($channel, $keyTime);

		// Output div
		print "<div class='hawthorn_linktochat' style='display:none' " .
			"id='hawthorn_linktochat$index'>\n";
		print "<script type='text/javascript'>document.getElementById(" .
			"'hawthorn_linktochat$index').style.display='block';</script>\n";

		// Output link
		print "<a href='#' onclick=\"hawthorn.openPopup('" .
			self::escapeJS($this->popupUrl) . "','" .
			self::escapeJS($this->reAcquireUrl) . "','$channel'," .
			"'$this->user','" . self::escapeJS($this->displayName) . "'," .
			"$time,'$key','" . self::escapeJS($title) . "');\">\n";

		// Print content
		print $linkText;

		// Print icon if provided
		if ($icon)
		{
			if (!$iconAlt)
			{
				$iconAlt = 'Opens in new window';
			}
			print " <img src='" . self::escapeXML($icon) . "' alt='" . self::escapeXML($iconAlt) . "' title='" .
				self::escapeXML($iconAlt) . "' />\n";
		}

		// Close tags
		print "</a></div>\n";

		// Print script tag
		$this->printJS();
	}

	/**
	 * Obtains a raw authorisation key that allows access to a channel
	 * for one hour (or time specified in constructor).
	 * @param string $channel Hawthorn channel ID
	 * @param string &$key Out parameter: receives Hawthorn key
	 * @param string &$keyTime Out parameter: receives key expire time
	 * @throws Exception If channel name is invalid
	 */
	function getAuthKey($channel, &$key, &$keyTime)
	{
		$keyTime = $this->getKeyTime();
		$key = $this->getKey($channel, $keyTime, true);
	}

	/**
	 * Prints JavaScript code that should run in response to a Hawthorn
	 * re-acquire request.
	 * The user's authorisation to the underlying system should be
	 * rechecked before calling this method.
	 * This method also sets up the page header.
	 * @param string $id Value of 'id' parameter passed to the re-acquire
	 *  page
	 * @param string $channel Hawthorn channel ID ('channel' parameter)
	 * @throws Exception If channel name is invalid
	 */
	function reAcquireAllow($id, $channel)
	{
		// Get auth key
		$keyTime = $this->getKeyTime();
		$key = $this->getKey($channel, $keyTime);

		// Send header
		header("Content-Type: text/javascript; charset=UTF-8");

		// Print JavaScript
		print "hawthorn.reAcquireComplete('$id','$key','$keyTime');\n";
	}

	/**
	 * Prints JavaScript code that should run in response to a Hawthorn
	 * re-acquire request which has failed. If the authorisation attempt
	 * to the underlying system fails, this method should be called.
	 * The user's authorisation to the underlying system should be
	 * rechecked before calling this tag.
	 * This method also sets up the page header.
	 * @param string $id Value of 'id' parameter passed to the re-acquire
	 *  page
	 * @param string $error Error message to return
	 */
	function reAcquireDeny($id, $error)
	{
		// Send header
		header("Content-Type: text/javascript; charset=UTF-8");

		// Print JavaScript
		print "hawthorn.reAcquireDeny('$id','" .
			self::escapeJS($error) . "');\n";
	}

	/**
	 * Provides a link to the server statistics page for each server. The
	 * links are formatted as an unordered list. The top level ul tag has
	 * class hawthorn_statslinks. Note that the auth key also gives access
	 * to the system log, so this method should only be run for system
	 * administrators.
	 */
	function linkToStatistics()
	{
		// Get auth key for special admin user
		$keyTime = $this->getKeyTime();
		$key = $this->getKey("!system", $keyTime, true, "_admin", "_");

		// Output list
		print "<ul class='hawthorn_statslinks'>\n";
		foreach ($this->servers as $server)
		{
			print "<li><a href='" . self::escapeXML($server) . "hawthorn/html/statistics?channel=!system&amp;" .
				"user=_admin&amp;displayname=_&amp;keytime=$keyTime&amp;" .
				"key=$key'>" . self::escapeXML($server) . "</a></li>\n";
		}
		print "</ul>\n";
	}

	// Private implementation functions
	///////////////////////////////////

	/**
	 * Obtains an authorisation key.
	 * @param string $channel Channel ID
	 * @param string $keyTime Key expiry time in milliseconds
	 * @param bool $allowSystem True to allow special system channel
	 * @param string $user User ID (blank to use value set in constructor)
	 * @param string $displayName Display name (blank for default)
	 * @return string Key
	 * @throws Exception If channel name is invalid
	 */
	private function getKey($channel, $keyTime, $allowSystem = false,
		$user = '', $displayName = '')
	{
		// Check channel is valid
		if (!preg_match('~^[A-Za-z0-9_]+$~',$channel) &&
			($channel!=='!system' || !$allowSystem))
		{
			throw new Exception("Invalid channel ID: $channel");
		}

		// Sort out user/display name
		if ($user === '')
		{
			$user = $this->user;
		}
		if ($displayName === '')
		{
			$displayName = $this->displayName;
		}

		// Work out key
		$hashData = "$channel\n$user\n$displayName\n" .
			"$keyTime\n$this->magicNumber";
		return sha1($hashData);
	}

	/** @return Key time (current time plus expiry) in milliseconds */
	private function getKeyTime()
	{
		// PHP doesn't have long, so use int calculation
		// then stick 000 on the end as a string.
		return (int)(time() + $this->keyExpiry/1000).'000';
	}

	/**
	 * Escapes a string suitable for inclusion within JS single quotes.
	 * @param string $text String to escape
	 * @return string String with some characters escaped
	 */
	private static function escapeJS($text)
	{
		return str_replace('\'', '\\\'', str_replace('\\', '\\\\', $text));
	}

	/**
	 * Escapes a string suitable for inclusion within HTML
	 * @param string $text String to escape
	 * @return string String with some characters escaped
	 */
	private static function escapeXML($text)
	{
		return htmlspecialchars($text, ENT_QUOTES);
	}

	/**
	 * Prints a link to the main Hawthorn JS file, and includes a call to
	 * the hawthorn.init method. Does nothing if JS has already been
	 * printed.
	 * @param bool $evenDeferred If true, prints now even when defer was set
	 */
	private function printJS($evenDeferred = false)
	{
		if ((!$this->defer || $evenDeferred) && !$this->printedJS)
		{
			$this->printedJS = true;
			print "<script type='text/javascript' src'" .
				self::escapeXML($this->jsUrl) . "'></script>\n";
			print "<script type='text/javascript'>\n";
			print "hawthorn.init([";
			$first = true;
			foreach ($this->servers as $server)
			{
				if($first)
				{
					$first = false;
					print "'";
				}
				else
				{
					print ",'";
				}
				print $server;
				print "'";
			}
			print "]);\n";
			print "</script>";
		}
	}

}
?>