This project is not ready for use yet. When it is I will update this README.

TODO:
* Add installation/management documentation.
* Add documentation for writing connectors in JSP and PHP.

Testing TODO:
* HTML example
* JSP example
* PHP example
* Multiple servers

-- The below is the 'real' intended contents of this README --

Welcome to Hawthorn!

Please read the Hawthorn manual, hawthorn.pdf, before attempting to deploy
the Hawthorn system. It introduces all the required concepts (some of which
are quite confusing).

The Hawthorn distribution contains the following folders and contents. Note
that each folder has its own readme.txt that explains the content of that
folder in more detail.

- bin

  Hawthorn server binary.
  
  Hawthorn load-testing binary.
  
- connectors

  Plug-ins for third party systems which connect Hawthorn into those systems
  so that users of those systems can use Hawthorn chat.
  
  Example plug-ins written in different server-side scripting languages to
  assist developers of new connector plug-ins.

- lib

  Library code for developers of new connector plug-ins (this code is also
  included in the connector examples in connectors/).

- src

  Full Java source code, for reference use. See src/readme.txt for more
  information.

  Javadoc source-code documentation.
