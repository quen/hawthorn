<?php
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
		// Check access
		$context = get_context_instance(CONTEXT_BLOCK, $this->instance->id);
		if (!has_capability('block/hawthorn:chat', $context))
		{
			// Don't show block at all
			return;
		}
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
		// Load Hawthorn library
		// TODO This link should point to a copy of the same file, in the built
		// version.
		require_once(dirname(__FILE__) . '/../../php/hawthorn.php');
		$hawthorn = new hawthorn($CFG->block_hawthorn_magicnumber,
			explode(',',$CFG->block_hawthorn_servers), hawthorn::escapeId($USER->username),
			fullname($USER), $permissions,
			$CFG->wwwroot . '/blocks/hawthorn/hawthorn.js',
			$CFG->wwwroot . '/blocks/hawthorn/popup.php',
			$CFG->wwwroot . '/blocks/hawthorn/reacquire.php');
		// TODO Use group options
		$channel = 'c' . $COURSE->id;
		$this->content->text = $hawthorn->recent($channel, 3, 900000, 5, 3,
		  get_string('recent', 'block_hawthorn'),
		  get_string('names', 'block_hawthorn'),
		  get_string('loading', 'block_hawthorn'),
			get_string('noscript', 'block_hawthorn'));
		$this->content->text .= $hawthorn->linkToChat($channel,
			get_string('coursechat', 'block_hawthorn', $COURSE->shortname),
			get_string('coursechatlink', 'block_hawthorn', $COURSE->shortname));
		$this->content->footer = '';
  }
}
?>