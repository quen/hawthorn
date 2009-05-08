<?php
define('HAWTHORN_DEFAULTMESSAGES', 3);
define('HAWTHORN_DEFAULTNAMES', 5);

class block_hawthorn extends block_base
{
	function init()
	{
		$this->title = get_string('blockname','block_hawthorn');
		$this->version = 2009050400;
	}
	function instance_allow_config()
	{
		// Yes, there is a configuration page
		return true;
	}
	function has_config()
	{
		// Yes, there is a global configuration page
		return true;
	}
	function user_can_addto(&$page) {
		// Only available on course pages
		return $page->type === 'course-view';
	}
	function get_content()
	{
		if(!empty($this->content->text)) {
			return;
		}
		$this->content = new stdClass;
		$this->content->footer = '';

		// Get course object
		global $USER, $CFG, $COURSE;
		if ($this->instance->pageid == $COURSE->id)
		{
			$course = $COURSE;
		}
		else
		{
			$course = get_record('course', 'id', $this->instance->pageid);
		}

		// Check access
		$context = get_context_instance(CONTEXT_COURSE, $course->id);
		if (!has_capability('block/hawthorn:chat', $context))
		{
			// Don't show block at all
			return;
		}

		// Work out user permissions
		$permissions = 'rw';
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
		
		// Get name and message options
		$maxmessages = isset($this->config->maxmessages)
			? $this->config->maxmessages : HAWTHORN_DEFAULTMESSAGES;
		$maxnames = isset($this->config->maxnames)
			? $this->config->maxnames : HAWTHORN_DEFAULTNAMES;

		// Decide key expiry (ms). Usually 1 hour, unless session timeout is lower.
		$keyExpiry = 3600000;
		if($CFG->sessiontimeout*1000 < $keyExpiry)
		{
			// Set expiry to session timeout (note that the JS will make a re-acquire
			// request 5 minutes before this)
			$keyExpiry = $CFG->sessiontimeout*1000;

			// Ridiculously short sessions are going to cause problems.
			if($CFG->sessiontimeout < 10 * 60)
			{
				$this->content->text = get_string('error_sessions', 'block_hawthorn');
				return;
			}
		}

		// Load Hawthorn library.
		// IF THIS LINE FAILS BECAUSE THE FILE IS NOT PRESENT: This is the
		// standard PHP connector library. It is not duplicated in the source tree.
		// The build script makes a copy of this file (connectors/php/hawthorn.php)
		// into the Moodle block folder so that this reference works.
		require_once($CFG->dirroot . '/blocks/hawthorn/hawthorn.php');

		// Initialise
		$hawthorn = new hawthorn($CFG->block_hawthorn_magicnumber,
			explode(',',$CFG->block_hawthorn_servers), hawthorn::escapeId($USER->username),
			fullname($USER), $userpic, $permissions,
			$CFG->wwwroot . '/blocks/hawthorn/hawthorn.js',
			$CFG->wwwroot . '/blocks/hawthorn/popup.php',
			$CFG->wwwroot . '/blocks/hawthorn/reacquire.php',
			false, $keyExpiry);
		// TODO Use group options
		$channel = 'c' . $COURSE->id;
		$this->content->text = '';
		$this->content->text .= $hawthorn->linkToChat($channel,
			get_string('coursechat', 'block_hawthorn', $COURSE->shortname),
			get_string('coursechatlink', 'block_hawthorn', $COURSE->shortname));
		if($maxmessages>0 || $maxnames>0)
		{
			$this->content->text .= $hawthorn->recent($channel, $maxmessages, 900000,
				$maxnames, 3,
			  get_string('recent', 'block_hawthorn'),
			  get_string('names', 'block_hawthorn'),
			  get_string('loading', 'block_hawthorn'),
				get_string('noscript', 'block_hawthorn'));
		}
  }
}
?>