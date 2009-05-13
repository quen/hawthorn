Requirements
------------

* Application server with JSP 2.1
* Java 1.5+

                        +-------------------------+
                        |                         |
                        |    BIG SCARY WARNING    |
                        |                         |
                        +-------------------------+

-> THIS IS ONLY AN EXAMPLE! Do not deploy this test on a production server! 
   It allows full access to a Hawthorn chat channel, without any authentication.

-> You need to make a change to server configuration if you are using Tomcat,
   and probably also for other application servers!

-> I haven't found any way to make this change at webapp level. It needs to
   be changed at server level.

   
How to change Tomcat configuration
----------------------------------

Instructions are for 6.x.

1) Open the server.xml configuration file.

2) Look for the <Connector> tag or tags. (If there's more than one, change
   them all.)

3) The tag has many attributes such as port, protocol, etc. Add the following 
   new attribute into the tag:

   URIEncoding="UTF-8"

If you don't do this, the system will break (most likely during the re-acquire
process, so it might not be immediately obvious) if anyone has a display name
with any non-ASCII characters (such as an accented letter). Since display names
are supposed to allow any character, this is a serious problem.

The reason is that, being a JSON application, Hawthorn needs to use GET 
rather than POST requests. These requests are sent in UTF-8 character encoding.
Web applications can set the correct character encoding for received POST
requests, but (at least in Tomcat) this doesn't work for GET requests - it
must be set at the server level.


How to use this example
-----------------------

1) If your server does not already have them installed elsewhere, add these
   standard JSTL files:

   jstl.jar
   standard.jar

   into the WEB-INF/lib folder.

2) Edit test.jsp and update the 'magicnumber' and 'servers' parameters on 
   <hawthorn:init> so that they match your Hawthorn server settings.

2) Visit test.jsp in your Web browser. Enter any valid user name, display name,
   arbitrary extra data (probably just leave this blank), and permissions.

3) Submit the form. The resulting page includes various information about
   the channel and a link to open a Hawthorn chat popup. If you specify
   admin permission (a), it also includes a link to the server statistics page.
