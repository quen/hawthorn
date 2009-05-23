<?php
/*
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
*/

require_once('hawthorn.php');

// This script should now check that the user is authorised to chat and has
// the specified permissions. (Or ignore permissions/user etc in the request
// and generate them afresh.) In this test version, we assume that the user
// is authorised.

$hawthorn = new hawthorn('23d70acbe28943b3548e500e297afb16',
	array('http://192.168.0.100:13370/'),
	$_GET['user'], $_GET['displayname'], $_GET['extra'],
	$_GET['permissions'], 'hawthorn.js', 'popup.html', 'reacquire.php');

// Allow access
$hawthorn->reAcquireAllow($_GET['id'], $_GET['channel']);
?>