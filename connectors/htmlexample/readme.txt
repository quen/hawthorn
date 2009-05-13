Note: This pure-HTML example is only for use in testing, and as a development
example.

                        +-------------------------+
                        |                         |
                        |      CONFUSING FACT     |
                        |                         |
                        +-------------------------+

It is NOT POSSIBLE to connect to a Hawthorn server using only HTML and
JavaScript. You must have a server-side programming environment such as PHP
or JSP.

This is because you need a Hawthorn magic number to generate keys for
users, and that magic number must be kept absolutely secret (otherwise
every user can impersonate every other user). Consequently you cannot pass
this magic number to JavaScript code, which runs on user browsers and would
give users access to the number.

Requirements
------------

* A Web browser.

How to use this example
-----------------------

1) Open index.html in your Web browser.

2) Add one or more <testkey> tags into your Hawthorn server configuration file
   and launch the server.

3) The server URL and test user details will be displayed on standard output.
   Copy and paste these details into the HTML form. Follow instructions on
   the form to try out Hawthorn features.
