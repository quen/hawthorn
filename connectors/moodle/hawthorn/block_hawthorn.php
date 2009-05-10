<?php
define('HAWTHORN_DEFAULTMESSAGES', 3);
define('HAWTHORN_DEFAULTNAMES', 5);
define('HAWTHORN_DEFAULTMODE', 'course');

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

	function user_can_addto(&$page)
	{
		// Only available on course pages
		return $page->type === 'course-view';
	}

	function get_content()
	{
		if(!empty($this->content->text))
		{
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

		// Check group mode
		$mode = isset($this->config->mode)
			? $this->config->mode : HAWTHORN_DEFAULTMODE;

		// Check whether to show course part
		$coursepart = $mode == 'course' || $mode == 'both';

		// Check whether to show group part
		$grouppart = false;
		if($mode == 'group' || $mode == 'both')
		{
			// Note: The below code cannot use standard Moodle groups functions
			// because these include the concept of viewing 'all groups', which
			// you can't do with Hawthorn. Also we don't support visible groups
			// (at the moment).

			// Find out which group(s) the current user can access
			$grouping = isset($this->config->grouping) ? $this->config->grouping : 0;
			$groupuser = $USER->id;
			$mygroups = groups_get_all_groups($COURSE->id, $USER->id, $grouping);
			$mygroups = $mygroups ? $mygroups : array();
			if(has_capability('moodle/site:accessallgroups', $context))
			{
				$groups = groups_get_all_groups($COURSE->id, 0, $grouping);
				$groups = $groups ? $groups : array();
			}
			else
			{
				$groups = $mygroups;
			}

			// Any groups at all?
			$grouppart = (count($groups) > 0);
		}

		// Build display output
		$this->content->text = '';
		$headinglevel = ($grouppart && $coursepart) ? 4 : 3;

		// Course part
		$coursechunk = '';
		if($coursepart)
		{
			// Course chat heading shown only if there are both options
			if($grouppart)
			{
				$this->content->text .= '<h3>' .
					get_string('courseheading', 'block_hawthorn') . '</h3>';
			}

			// Actual course channel content
			$channel = 'c' . $COURSE->id;
			$this->content->text .= $hawthorn->linkToChat($channel,
				get_string('coursechat', 'block_hawthorn',
					format_string($COURSE->shortname)),
				get_string('coursechatlink', 'block_hawthorn',
					format_string($COURSE->shortname)));
			if($maxmessages>0 || $maxnames>0)
			{
				$this->content->text .= $hawthorn->recent($channel, $maxmessages, 900000,
					$maxnames, $headinglevel,
				  get_string('recent', 'block_hawthorn'),
				  get_string('names', 'block_hawthorn'),
				  get_string('loading', 'block_hawthorn'),
					get_string('noscript', 'block_hawthorn'));
			}
		}

		if($grouppart)
		{
			// Group chat heading shown only if there are both options
			if($coursepart)
			{
				$this->content->text .= '<h3 class="hawthorn_secondheading">' .
					get_string('groupheading', 'block_hawthorn') . '</h3>';
			}

			// Find selected group
			$selectedgroup = 0;
			if(isset($SESSION->block_hawthorn_group) &&
				array_key_exists($COURSE->id, $SESSION->block_hawthorn_group))
			{
				$selectedgroup = $SESSION->block_hawthorn_group[$COURSE->id];
			}
			if($change = optional_param('hawthorngroup', 0, PARAM_INT))
			{
				$selectedgroup = $change;
				$SESSION->block_hawthorn_group[$COURSE->id] = $selectedgroup;
			}
			if(!array_key_exists($selectedgroup, $groups))
			{
				// Not allowed to select groups that don't exist
				$selectedgroup = 0;
			}
			if(!$selectedgroup)
			{
				if(count($mygroups) > 0)
				{
					reset($mygroups);
					$selectedgroup = key($mygroups);
				}
				else
				{
					reset($groups);
					$selectedgroup = key($groups);
				}
			}

			// Group selector
			if(count($groups) > 1)
			{
				foreach($groups as $group)
				{
					$groupsmenu[$group->id] = format_string($group->name);
				}
				$this->content->text .= popup_form($CFG->wwwroot .
					'/course/view.php?id=' . $COURSE->id . '&amp;hawthorngroup=',
					$groupsmenu, 'hawthorngroupselector', $selectedgroup, '', '', '',
					true);
			}

			// Actual link to chat and recent messages
			$channel = 'g' . $selectedgroup;
			$this->content->text .= $hawthorn->linkToChat($channel,
				get_string('groupchat', 'block_hawthorn', (object)array(
					'course' => format_string($COURSE->shortname),
					'group' => format_string($groups[$selectedgroup]->name))),
				get_string('groupchatlink', 'block_hawthorn',
					format_string($groups[$selectedgroup]->name)));
			if($maxmessages>0 || $maxnames>0)
			{
				$this->content->text .= $hawthorn->recent($channel, $maxmessages, 900000,
					$maxnames, $headinglevel,
				  get_string('recent', 'block_hawthorn'),
				  get_string('names', 'block_hawthorn'),
				  get_string('loading', 'block_hawthorn'),
					get_string('noscript', 'block_hawthorn'));
			}
		}
	}
}
?>