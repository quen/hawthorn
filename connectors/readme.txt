Hawthorn connectors
-------------------

Please see readme.txt inside the connector you're interested in for
installation instructions and warnings.

NOTE: All these connectors require files from elsewhere in the project.
These files have automatically been copied into place in the Hawthorn
release distribution. Please use these folders from the Hawthorn distribution
and not directly from the source control system. (If you want to build from
source, use the build.xml Ant script to generate a release distribution.)

* htmlexample

  Not a true connector. Demonstrates basic usage of the Hawthorn JavaScript.

  You can use this:

  - to test an installed server, after obtaining a key using the <testkey>
    tag within the server configuration file.

  - as a base to see how to make connectors from scratch (if you want to use
    an environment other than JSP or PHP, or you don't like the provided
    libraries for those environments).

* jspexample

  Demonstrates usage of the included JSP library. JSP developers can use this
  as a base for integrating with their own systems.

* phpexample

  Demonstrates usage of the included PHP library. PHP developers can use this
  as a base for integrating with their own systems.

* moodle

  Fully-functional connector for the Moodle virtual learning environment:
  http://www.moodle.org/

  This connector acts as a Moodle block. You can add it to any course to
  enable Hawthorn chat.
