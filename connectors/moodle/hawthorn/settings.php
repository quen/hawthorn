<?php
require_once($CFG->dirroot . '/blocks/hawthorn/hawthornlib.php');

$settings->add(new admin_setting_configtext('block_hawthorn_magicnumber',
	get_string('magicnumber', 'block_hawthorn'),
	get_string('configmagicnumber', 'block_hawthorn'), '', PARAM_TEXT));

$settings->add(new admin_setting_configtext('block_hawthorn_servers',
	get_string('servers', 'block_hawthorn'),
	get_string('configservers', 'block_hawthorn'), '', PARAM_TEXT));

$sitecourse = (object)array('id' => SITEID);
$hawthorn = get_hawthorn($sitecourse);
$settings->add(new admin_setting_heading('hawthorn_statslink',
	get_string('statisticsheading', 'block_hawthorn'),
	$hawthorn->linkToStatistics()));
?>