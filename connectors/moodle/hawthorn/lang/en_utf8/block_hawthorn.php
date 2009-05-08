<?php
// Block name
$string['blockname'] = 'Chat';

// Capabilities
$string['hawthorn:admin'] = 'Admin access to Hawthorn chat system';
$string['hawthorn:chat'] = 'Participate in Hawthorn chat';
$string['hawthorn:moderate'] = 'Moderate Hawthorn chat';

// Global settings
$string['magicnumber'] = 'Hawthorn magic number';
$string['configmagicnumber'] = 'A number used to authorise Hawthorn system
  access. Must be identical to the magic number configured on the Hawthorn
  server. <br /><strong>This number has to be kept absolutely secret!</strong>';
$string['servers'] = 'Hawthorn server URLs';
$string['configservers'] = 'The URL of the Hawthorn server, ending in a /.
  Use a comma-separated list if you have multiple servers.';

// Instance settings
$string['mode'] = 'Chat with';
$string['mode_course'] = 'Entire course';
$string['mode_group'] = 'Own group';
$string['mode_both'] = 'Course or group';
$string['showmessages'] = 'Show recent messages';
$string['nomessages'] = 'Do not display messages';
$string['one_message'] = 'Up to 1 message';
$string['n_messages'] = 'Up to $a messages';
$string['shownames'] = 'Show users present';
$string['nonames'] = 'Do not display names';
$string['n_names'] = 'Up to $a names';

// Main text
$string['coursechat'] = '$a: Chat';
$string['coursechatlink'] = 'Open chat for $a';
$string['loading'] = '(Loading chat information, please wait...)';
$string['noscript'] = '(Chat features are not available because
	JavaScript is disabled.)';
$string['recent'] = 'Recent messages';
$string['names'] = 'People in chat';

// Popup text
$string['popup_joined'] = ' joined the chat';
$string['popup_left'] = ' left the chat';
$string['popup_banned'] = ' banned user: ';
$string['popup_error'] = 'A system error occurred';
$string['popup_confirmban'] = 'Are you sure you want to ban $1?\n\nBanning ' .
	'means they will not be able to chat here, or watch this chat, for the ' .
	'next 4 hours.';
$string['popup_closechat'] = 'Close chat';
$string['popup_intro'] = 'To chat, type messages in the textbox and press ' .
  'Return to send.';
$string['popup_banuser'] = 'Ban user';

// Errors
$string['error_invalid'] = 'Invalid parameters';
$string['error_sessions'] = 'Your Moodle session timeout is set very low. '.
	'Hawthorn requires session timeouts of at least 15 minutes. Please increase '.
	'the session timeout (admin option <strong>sessiontimeout</strong>) in '.
	'order to use Hawthorn chat.';
?>