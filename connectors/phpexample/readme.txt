Requirements
------------

* PHP 5.x

                        +-------------------------+
                        |                         |
                        |    BIG SCARY WARNING    |
                        |                         |
                        +-------------------------+

-> THIS IS ONLY AN EXAMPLE! Do not deploy this test on a production server!
   It allows full access to a Hawthorn chat channel, without any authentication.


How to use this example
-----------------------

1) Edit test.php and update the 'magicnumber' and 'servers' parameters on
   <hawthorn:init> so that they match your Hawthorn server settings.

2) Visit test.php in your Web browser. Enter any valid user name, display name,
   arbitrary extra data (probably just leave this blank), and permissions.

3) Submit the form. The resulting page includes various information about
   the channel and a link to open a Hawthorn chat popup. If you specify
   admin permission (a), it also includes a link to the server statistics page.
