<?php
require_once('../../config.php');
require_once($CFG->dirroot . '/blocks/hawthorn/hawthornlib.php');

// Get and check channel and id parameters. We ignore other parameters.
$channel = optional_param('channel', '', PARAM_RAW);
$matches = array();
if(!preg_match('~^([cg])([0-9]{1,18})$~', $channel, $matches))
{
	print_error('error_invalid', 'block_hawthorn');
}
list($junk, $type, $id) = $matches;
$requestid = optional_param('id', '', PARAM_RAW);
if(!preg_match('~^[0-9]{1,18}$~', $requestid))
{
	print_error('error_invalid', 'block_hawthorn');
}

// Get course ID
if($type == 'c')
{
	$courseid = $id;
}
else // 'g'
{	
	$courseid = get_field('groups', 'courseid', 'id', $id);
} 

// Note: Calling the below 'require' functions is sort of bad practice because
// they will print HTML and exit, rather than sending the Hawthorn deny-access
// JavaScript. However, it's easier to do things this way in Moodle, and
// Hawthorn will still display an error to users if the key expires.

// Require course login
require_login($courseid);
$context = get_context_instance(CONTEXT_COURSE, $courseid);

// Require group access
if($type == 'g')
{
	$ismember = count_records('groups_members', 'groupid', $id, 
		'userid', $USER->id) == 1;
	if(!$ismember)
	{
    if($COURSE->groupmode != VISIBLEGROUPS)
    {
    	require_capability('moodle/site:accessallgroups', $context);
    }
	}
}

// OK, access was permitted.

// Initialise Hawthorn
$hawthorn = get_hawthorn();

// Allow access
$hawthorn->reAcquireAllow($requestid, $channel);
?>