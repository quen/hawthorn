OS X installation
-----------------

Please see the manual for platform-agnostic installation instructions.

After you have followed those instructions (you've set up the program, user,
group, configuration file, and log folder), you can use the instructions here
to make Hawthorn run automatically when your server starts up.


IMPORTANT NOTE
* This script has only been tested on OS X 10.5.7.
* It may not work, or need modification, on other versions.


Setup
-----

To set up Hawthorn to start with your OS X server:

1) Copy the Hawthorn folder from this folder into /Library/StartupItems.

2) Edit the Hawthorn script so that APPFILE and CONFIGFILE point to the
correct locations. PIDFILE will be used to store the program ID; you can
put this where you like, but probably inside the Hawthorn log folder would
be sensible.

3) Change permissions as follows:

sudo chmod a+x /Library/StartupItems/Hawthorn/Hawthorn


Basic testing
-------------

Test as follows:

$ sudo SystemStarter start Hawthorn
Starting Hawthorn (pid 7051)

$ sudo SystemStarter stop Hawthorn
Stopping Hawthorn (pid 7051)

If you don't get a response like the above, something is wrong. Check the
file permissions and folders.

Then look at the Hawthorn logs. Here's the command I used; the logfile will
have a different name on your machine.

sudo cat /UnixData/hawthorn-logs/\!system.192.168.0.100_13370.2009-05-16.log

You should see startup information at the end of the file. If you're not
sure, restart Hawthorn using the above commands and check the file again to
make sure that new information has appeared.


Final test
----------

Restart your computer. When it comes back up, check the logfile to see that
Hawthorn has started. You can also use ps:

$ ps -ax | grep hawthorn
 7110 ??         0:00.38 /usr/bin/java -server -d64 -Xmx256M -Xms256M -jar
	/UnixApps/hawthorn/hawthorn.jar /UnixData/hawthorn-conf/config.xml
 7112 ttys001    0:00.00 grep hawthorn


Changing Hawthorn config
------------------------

Should you need to change Hawthorn configuration, edit the file and then
run:

sudo SystemStarter restart Hawthorn
