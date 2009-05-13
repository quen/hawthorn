<!DOCTYPE html
	PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
Copyright 2009 Samuel Marshall

This file is part of Hawthorn.
http://www.leafdigital.com/software/hawthorn/

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
-->
<html>
<head>
<title>Hawthorn PHP example (minimal)</title>
</head>
<body>

<p><strong>This script must not be deployed on a live server.</strong> It
allows anyone to obtain any permissions on the Hawthorn server.</p>

<?php 
require_once('hawthorn.php');

if(array_key_exists('user', $_GET))
{
	// In a real system you would fill the values in here from:
	// 1) Magic number from your system's configuration, which would be set to
	//    match the Hawthorn server's magic number. (Do not use this example
	//    magic number on any live system!)
	// 2) User and display name from your system's user database, based on
	//    the current authenticated user.
	// 3) Permissions from your system's user database, based on permission
	//    information your system stores ("rw" for normal users).
	// 4) Hawthorn server URL(s) from your system's configuration.
	$hawthorn = new hawthorn('23d70acbe28943b3548e500e297afb16',
		array('http://192.168.0.100:13370/'),
		$_GET['user'], $_GET['displayname'], $_GET['extra'],
		$_GET['permissions'], 'hawthorn.js', 'popup.html', 'reacquire.php');

	// This test uses a load test channel in case you want to watch a load test.

	// To show it working, just get the auth key and display it.
	$hawthorn->getAuthKey('loadtestchan3', $hawthornKey, $hawthornKeyTime);
?>
<p>Ok, got past init again. Key is <?php print $hawthornKey; ?>, time
<?php print $hawthornKeyTime; ?>.</p>
<?php
	// Print JS code that causes recent messages to be displayed.
	print $hawthorn->recent('loadtestchan3');

	// Print link to chat in channel.
	print $hawthorn->linkToChat('loadtestchan3', 'Load test channel 3',
		'Chat now!');

	// For admins only, print server statistics link
	$isAdmin = (strpos('a', $_GET['permissions']) !== false);
	if($isadmin)
	{
		print $hawthorn->linkToStatistics();
	}
}
else
{
?>
<form method="get" action="test.php">
<div>
Username (lowercase letters/numbers only)
<input type="text" name="user" />
</div>
<div>
Display name (any text)
<input type="text" name="displayname" />
</div>
<div>
Extra data (any text, may be empty)
<input type="text" name="extra" />
</div>
<div>
Permissions (full permissions = rwma)
<input type="text" name="permissions" value="rw" />
</div>
<div>
<input type="submit" />
</div>

</form>

<?php
}
?>