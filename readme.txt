Welcome to Hawthorn!

If you are trying to deploy Hawthorn, please do not use this source tree.
Instead, please download a release distribution from the 'Downloads' tab:

http://github.com/quen/hawthorn/downloads

Alternatively, you can build your own release distribution by running the Ant
build script build.xml (see below).

If you want to know more about the system, please read the Hawthorn manual,
which is included in the release distribution or in this source tree as
doc/hawthorn.pdf. The manual explains what the system does and introduces
all the required concepts (some of which are quite confusing).

The Hawthorn source tree contains the following folders and contents.

- (root folder)

  This readme.

  The Ant build script build.xml

  License details, credits, information about contributing.

- src

  Java source code for server, load tester, and JSP tag library.

- connectors

  (NOTE: These files cannot be used as-is because they assume that files from
  other areas are copied in. This happens when building the distribution.)

  Plug-ins for third party systems which connect Hawthorn into those systems
  so that users of those systems can use Hawthorn chat.

  Example plug-ins written in different server-side scripting languages to
  assist developers of new connector plug-ins.

- doc

  Documentation files (final and source files) and some of the readme files
  used in building the release distribution.

- js

  JavaScript and related files for deployment to client browsers.

- lib

  (NOTE: These files cannot be used as-is because they assume that files from
  other areas are copied in. This happens when building the distribution.)

  Library code for developers of new connector plug-ins.

The main thing missing that you'll find in the distribution releases is
javadoc API documentation. This is built as part of the distribution. Download
or build a distribution release to access this documentation.


How to build the Hawthorn distribution
--------------------------------------

Requirements:

* Apache Ant 1.7+.
* Java 1.5+ compiler.
* Apache Tomcat or something else that includes the file jsp-api.jar.

To build from command line:

1. cd [project root folder]
2. ant -Djspapi=[full path to jsp-api.jar] build.xml

This will build the distribution in [home]/Desktop/hawthorn. It completely
wipes out any existing folder at that location, so run with care.

See build.xml if you want to change the target location or other properties.

You can also build directly inside most IDEs. To build in Eclipse:

1. Right-click on build.xml and choose 'Run as > Ant build...'
2. Change to Properties tab.
3. Turn off 'Use global properties'.
4. Click Add and add a property 'jspapi', set to the full path of jsp-api.jar.
5. Click Run.
