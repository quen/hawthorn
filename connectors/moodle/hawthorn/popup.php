<?php
require_once('../../config.php');
?>
<!DOCTYPE html
	PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<title>Hawthorn popup</title>
<link rel="stylesheet" type="text/css" href="hawthorn.css" />

<!-- Additional styles for user pics -->
<style type="text/css">
.entry .message
{
  margin-left:65px;
  text-indent:-25px;
}
.entry .userpic
{
  float:left;
}
</style>
</head>
<body>
<script type="text/javascript" src="hawthorn.js"></script>

<script type="text/javascript">
var popup = new HawthornPopup(false);

// Integrate Moodle language system
popup.strJoined = '<?php print addslashes_js(get_string('popup_joined', 'block_hawthorn')); ?>';
popup.strLeft = '<?php print addslashes_js(get_string('popup_left', 'block_hawthorn')); ?>';
popup.strBanned = '<?php print addslashes_js(get_string('popup_banned', 'block_hawthorn')); ?>';
popup.strError = '<?php print addslashes_js(get_string('popup_error', 'block_hawthorn')); ?>';
popup.strConfirmBan = '<?php print addslashes_js(get_string('popup_confirmban', 'block_hawthorn')); ?>';
popup.strCloseChat = '<?php print addslashes_js(get_string('popup_closechat', 'block_hawthorn')); ?>';
popup.strIntro = '<?php print addslashes_js(get_string('popup_intro', 'block_hawthorn')); ?>';
popup.strBanUser = '<?php print addslashes_js(get_string('popup_banuser', 'block_hawthorn')); ?>';

// Add user pictures
popup.addMessageExtra = function(type, extra, entry)
{
	var div = document.createElement('div');
	div.className = 'userpic';
	var img = document.createElement('img');
	div.appendChild(img);
	img.src = extra;
	img.alt = '';
	if(type == 'LEAVE')
	{
		img.style.opacity='0.3';
	}
	entry.insertBefore(div, entry.firstChild);
	div = document.createElement('div');
	div.style.clear = 'left';
	entry.appendChild(div);
};

popup.init();
</script>
</body>
</html>
