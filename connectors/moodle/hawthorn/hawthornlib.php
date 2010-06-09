<?php
// Requires Hawthorn library and includes utility function for creating
// an instance.

// IF THE NEXT LINE FAILS BECAUSE THE FILE IS NOT PRESENT: This is the
// standard PHP connector library. It is not duplicated in the source tree.
// The build script makes a copy of this file (connectors/php/hawthorn.php)
// into the Moodle block folder so that this call works. If you set this
// up without using the built version, manually copy this file into place.
require_once($CFG->dirroot . '/blocks/hawthorn/hawthorn.php');

/**
 * Creates a new Hawthorn object.
 * @param object $course Moodle course object. If not supplied, uses $COURSE.
 *   Only required field is ->id.
 */
function get_hawthorn($course = null)
{
	global $USER, $COURSE, $CFG;
	if($course == null)
	{
		$course = $COURSE;
	}
	$context = get_context_instance(CONTEXT_COURSE, $course->id);
  
	// Work out user permissions
	$permissions = '';
	if(has_capability('block/hawthorn:chat', $context))
	{
		$permissions .= 'rw';
	}
	if(has_capability('block/hawthorn:moderate', $context))
	{
		$permissions .= 'm';
	}
	if(has_capability('block/hawthorn:admin', $context))
	{
		$permissions .= 'a';
	}

	// Get user picture URL
	$userpic = print_user_picture($USER, $COURSE->id, NULL, 0, true, false);
	$userpic = preg_replace('~^.*src="([^"]*)".*$~', '$1', $userpic);

	// Decide key expiry (ms). Usually 1 hour, unless session timeout is lower.
	$keyExpiry = 3600000;
	if($CFG->sessiontimeout*1000 < $keyExpiry)
	{
		// Set expiry to session timeout (note that the JS will make a re-acquire
		// request 5 minutes before this)
		$keyExpiry = $CFG->sessiontimeout*1000;
	}

	// Get server list
	$servers = empty($CFG->block_hawthorn_servers)
		? array() 
		: explode(',',$CFG->block_hawthorn_servers);
	$magicnumber = empty($CFG->block_hawthorn_magicnumber)
		? 'xxx'
		: $CFG->block_hawthorn_magicnumber;

	// Construct Hawthorn object
	return new hawthorn(
		$magicnumber, $servers,
		hawthorn::escapeId($USER->username),
		fullname($USER), $userpic, $permissions,
		$CFG->wwwroot . '/blocks/hawthorn/hawthorn.js',
		$CFG->wwwroot . '/blocks/hawthorn/popup.php',
		$CFG->wwwroot . '/blocks/hawthorn/reacquire.php',
		false, $keyExpiry);
}
?>