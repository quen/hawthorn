<?php
define('HAWTHORN_DEFAULTMESSAGES', 3);
define('HAWTHORN_DEFAULTNAMES', 5);

class block_hawthorn extends block_base
{
	function init()
	{
		$this->title = get_string('blockname','block_hawthorn');
		$this->version = 2009050800;
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

		// Get name and message options
		$maxmessages = isset($this->config->maxmessages)
			? $this->config->maxmessages : HAWTHORN_DEFAULTMESSAGES;
		$maxnames = isset($this->config->maxnames)
			? $this->config->maxnames : HAWTHORN_DEFAULTNAMES;

		// Ridiculously short sessions are going to cause problems.
		if($CFG->sessiontimeout < 10 * 60)
		{
			$this->content->text = get_string('error_sessions', 'block_hawthorn');
			return;
		}

		// Load Hawthorn library.
		require_once($CFG->dirroot . '/blocks/hawthorn/hawthornlib.php');

		// Initialise
		$hawthorn = get_hawthorn($course);

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