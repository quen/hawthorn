Requirements
------------

* Application server with JSP 2.1
* Java 1.5+

                        +-------------------------+
                        |                         |
                        |    BIG SCARY WARNING    |
                        |                         |
                        +-------------------------+

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


How to connect your JSP application to Hawthorn
-----------------------------------------------

There is an example application in connectors/jspexample. Here are the
minimum steps:

1) Include tag library (hawthorn.taglib.jar) in your application's WEB-INF/lib.

2) Add code using this library to the page where you want to provide a link
   to chat.
   
3) Create a new script for Hawthorn chat to use when keys run out; this must
   checks that a user is authenticated, and if so, provide them with a new
   key.

Your application will likely need to include the client-side files hawthorn.js
and hawthorn.css. Some applications may use the unmodified popup.html also,
while others may implement a custom popup.jsp to add extra features.
   
Detailed information can be found in the system manual.
