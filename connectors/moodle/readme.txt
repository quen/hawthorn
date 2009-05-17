Requirements
------------

* Moodle 1.9.x
* PHP 5.x


How to install this block
-------------------------

1) In your Moodle installation, open the 'blocks' folder. Copy the 'hawthorn'
   folder from this installation into that Moodle folder (so that it becomes
   blocks/hawthorn).

2) Visit the Moodle admin page. The block will be initialised. You will
   be given some settings to complete.

3) Enter your Hawthorn server URL(s) and the magic number from your Hawthorn
   configuration.
   

Using the block
---------------

You can add Hawthorn chat to any course by adding the 'Chat' block (it is
not called 'Hawthorn chat' because using the name of the system would likely
confuse users of most sites).

The block contains a link to enter chat.

Click on the settings icon of the block to choose:

* Whether there is a chat channel for the whole course, or for each individual
  group, or both. [If groupings are enabled, you can also choose which grouping
  to use for chat channels.]

* How many messages and user names are displayed in the block. (This depends
  on how much space you want the block to take up.)

To configure access to chat for particular roles, use the 'Define roles' page
in admin, and search for 'hawthorn' in the capability list. There are
capabilities corresponding to Hawthorn permissions; 'chat' is rw, 'moderate'
is m, and 'admin' is a.

To override these permissions for a particular course, use the *course* roles
page and not the *block* roles page. Hawthorn permissions are configured
at block level.


Statistics
----------

There is a link to view server statistics on the Hawthorn block settings admin
page.


Logs
----

This connector does not provide direct access to chat logs. Please access the
log files manually.

* Moodle user names are used directly as Hawthorn usernames. If they contain
  invalid characters, these are converted into Unicode of the form _002a.

* Channel names use the numerical IDs, as follows:
  c123    = Course channel for course with ID 123
  c123g47 = Group channel for group with ID 47, within course with ID 123
