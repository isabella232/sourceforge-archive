JETTY - Extensible Java HTTP Server. 
By Mort Bay Consulting Pty. Ltd.  http://www.mortbay.com



INSTALLATION
============
Jetty comes compiled and ready to go. Unpack the Jetty distribution 
to a shared locations (/usr/local/Jetty is a good spot).   

The environment variable JETTY_HOME should be set to this directory. 
For the rest of this README the unix convention of $JETTY_HOME refers 
to the installed directory path (%JETTY_HOME% on dos/NT/windows).

   Unix:
	cd /usr/local
	gunzip < Jetty-X.X.X.tgz | tar xfv -
	JETTY_HOME=/usr/local/Jetty
	export JETTY_HOME

   Win95,NT:
	c:
	cd "\Program Files"
        winzip Jetty-X.X.X.tgz
	set JETTY_HOME="c:\Program files\Jetty"


Make sure your CLASSPATH includes all the jar files in
the $JETTY_HOME/lib directory  eg.

   Unix:
	CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/javax.servlet.jar
        CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/com.mortbay.Jetty.jar
        CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/gnujsp.jar


CONFIGURATION
=============

Jetty configuration can be considered on two levels. As it is 
intended to be embedded in other applications, the primary
configuration method is to provide an implementation of
the interface com.mortbay.HTTP.HttpConfiguration.

For conveniance, several implementations of HttpConfiguration
are supplied with the release:

  com.mortbay.Jetty.Server
    Reads configuration information from java properties files.

  com.mortbay.Jetty.Demo
    A specialization of com.mortbay.Jetty.Server that runs the
    demonstration server(s) for Jetty.

  com.mortbay.Jetty.Server21 
    An old version of Server that works with configuration files 
    for versions prior to 2.2.x

  com.mortbay.Jetty.JRun
    A server that reads JRUN configuration files for servlets

  com.mortbay.HTTP.Configure.ServletServer
    A server that dynamically loads servlets from the current
    directory.

  com.mortbay.HTTP.Configure.FileServer
    A server that serves files from the current directory


The configuration file format for com.mortbay.Jetty.Server is
designed to be flexible and extensible (if a little verbose
and confusing).  It is based on the com.mortbay.Util.PropertyTree
class which treats property names as dot separated node names
in a tree of properties and handles wildcards similar to X 
resource files. A number of sample configuration files are
included with the release:

  JettyServer.prp 
    A template file for configuration files

  JettyDemo.prp
    The configuration files for the Jetty demonstration.
    This file is verbosely constructed without using any of
    the PropertyTree wildcard facilities.

  JettyDemoServlets.prp, JettyDemoAliases.prp, JettyDemo*.prp, ...
    There are several configuration files that are referenced
    by JettyDemo.prp.

  JettyMinimalDemo.prp
    This configuration file has the same semantic content as the
    the collection of JettyDemo.prp files, but uses the wildcard
    facilities of PropertyTree to abbriviate to a minimal file.

  JettyFastDump.prp
    A configuration for the Dump and dynamically loaded servlets 
    with all features enabled on to their most expensive settings.

  JettyFasterDump.prp
    A configuration for the Dump and dynamically loaded servlets 
    with only common features enabled with reasonable settings.

  JettyFastestDump.prp
    A configuration for the Dump and dynamically loaded servlets
    with minimal features enabled.




RUNNING JETTY
=============

Running the Demo
----------------
The demonstration server has relative paths configured, so
it must be run from the $JETTY_HOME directory. Users should
consider using absolute file path names in their configurations
if they wish to run Jetty from any directory.

The demo server can be run with the following commands:

    cd $JETTY_HOME
    java com.mortbay.Jetty.Demo

Use a browser to access the Jetty demo site at:

    http://hostname:8080
  or
    http://127.0.0.1:8080   (if the browser is running on the same machine)



Running the Server
------------------
To run the server configured from a java properties file, run the following
commands.

    cd $JETTY_HOME
    java com.mortbay.Jetty.Server [configFile]

Example config files in $JETTY_HOME/etc include:

  JettyServer.prp 
  JettyFastDump.prp
  JettyFasterDump.prp
  JettyFastestDump.prp



Running a Servlet Server
------------------------

The com.mortbay.HTTP.Configure.ServletServer class uses the dynamic
servlet loading feature to load and run servlets without any
configuration.  Help for this class can be obtained by running

    java com.mortbay.HTTP.Configure.ServletServer -help

To run a servlet called mypackage.myservlet, change directories to 
the directory that contains the class file hierarchy (ie the parent 
directory of mypackage/myservlet.class ). Run the server with:

    java com.mortbay.HTTP.Configure.ServletServer 8080 / .

Access the server with a URL like
   
    http://hostname:8080/packagename.classname/pathInfo


If the servlet is in a jar file, run the server with

    java com.mortbay.HTTP.Configure.ServletServer 8080 / myclasses.jar

The server can be run with a search path for directoris and jar
files from which servlets are loaded. Note that the path syntax
is platform dependant and the unix for is used here:

    java com.mortbay.HTTP.Configure.ServletServer 8080 / .:myclasses.jar


Access the servlet at:

    http://hostname:8080/servlet/mypackage.myservlet


Loading and reloading of servlets can be controlled by accessing

    http://hostname:8080/LoaderServlet



Running a simple File Servler
-----------------------------
The com.mortbay.HTTP.Configure.FileServer class runs a file
server.  Help for this class can be obtained by running

   java com.mortbay.HTTP.Configure.FileServer -help

To run to serve files in the current directory:

   java com.mortbay.HTTP.Configure.FileServer 8080 

To run to serve files in another directory:

   java com.mortbay.HTTP.Configure.FileServer 8080 /other/directory




COMMON PROBLEMS
===============

Cannot read property file
-------------------------
Some JVMs appear to have problems reading property files that
mix their line termination characters.  The files in this release
have been created under Unix, so are terminated with LF. If you edit
these files under another OS, make sure you have a consistent line
termination character (CRLF for DOS & Windows, LF for Unix, CR for Mac).

GNUJSP locks during compile
---------------------------
The default GNUJSP configuration uses a command line compiler. Try
changing the compiler property for the gnujsp servlet to some of the
options suggested in the GNUJSP INSTALL file.

CLASSPATH does not work
-----------------------
Check that you use ':' or ';' as appropriate for your platform
when separating directories and jar files in CLASSPATHS.

PATHS does not work in config file
----------------------------------
For URL path lists, ';' or ',' should be used as a separator.


FREQUENTLY ASKED QUESTIONS
==========================

Does Jetty support SSL?
-----------------------
There is no Open Source java SSL implementation currently available 
(if there is, please tell me about it). As from Jetty-2.2, server
configuration can now specify the class to use for the HttpListener,
so integration of SSL should be fairly straight forward.  
Forge (www.forge.com.au) have donated a license for their Protekt
SSL implementation (a commercial product) and I may integrate that
at some stage.


Does Jetty support "standard" server side includes?
---------------------------------------------------
No. Jetty's server include tags are not standard, as they
pre-date the "standard" and use a very simple/fast parser.
We are waiting to see what happens with JSP,SML,etc. before
investing effort towards this.


Does Jetty support chained servlets?
------------------------------------
No. Chained servlets are a brain dead idea, as the servlet/request API
is not a filter style of API. Jetty provides filters which can be used
to modify the output of a servlet/file/forward/proxy request.
Jetty also supports the standard resource API that allows a servlet to
request the content from a URL. This works OK for accessing other
servlets and files.





MISCILLANEOUS
=============

Servlet Loading
---------------
Each servlet (and the classes it uses) is loaded in it's own ClassLoader
instance. The class loader uses the following search strategy when 
looking for classes:

 1) If the class is in the java.* javax.* or com.mortbay.* package
    hierarchy, it is loaded from the JVM loader. If it is not found,
    then the search does not continue.

 2) The class path configured for the servlet is searched for the
    class. The path may contain directories or jar files.

 3) If the class has not been found, the JVM loader is tried.
    Classes loaded from here cannot by dynamically reloaded.

Servlet reloading is implemented by discarding the servlets classloader
and all classes loaded by it. If a servlet instantiated a Thread, it
will continue to run on the original version of the class(es) until it
is restarted. It is the servlet implementors job to control the life
cycle of any such threads.


HTTP/1.1
--------
Servlets by default will only use persistent connections if
the content length is set or the chunked transfer encoding has
been set in the response header.  However, chunking can be used
by default if the java property CHUNK_BY_DEFAULT is set or 
ServletHolder.setChunkByDefault(true) is called, which allows
persistent connections to be used in more situations.



CONTACTS
========
    jetty-support@mortbay.com     - for problems with Jetty
    mortbay@mortbay.com           - for general contact
    jetty-announce@mortbay.com    - for the Jetty mailing list
                                    This is for announcements of interest
                                    to all Jetty users. It is not for
                                    support questions.
    subscribe-jetty-announce@mortbay.com   
                                  - join the mailing list



SUPPORT
=======
Mort Bay provides email support for Jetty. Infomal support is 
provided on a best effort basis (the better the effort of the 
reporter, the better the support...). 

Mort Bay also sells incident based support for Jetty.

If you are having a problem with Jetty, please supply the 
following information when sending email to jetty-support.

  Jetty version
  Java version
  Hardware platform.
  Operating System and version.
  Directory Jetty is installed in.
  Value of JETTY_HOME environment variable.
  Value of CLASSPATH environment variable.
  Command line used to run the server.
  Server properties file (if used and modified).



HOW CAN YOU CONTRIBUTE?
=======================

Any bug reports, performance hotspots and feature requests are welcomed.
However bug fixes, performance tweaks and extensions very warmly welcomed, 
and will get you into the CONTRIBUTORS list.

Any items in the TODO list are good targets for contribution, but you
should discuss them with us before doing too much.  We will soon
setup a jetty-discuss mailing list to discuss the future enhancements
of Jetty.

Documentation is any area where we would love additional contributions.
We are a little bit too close to the code to be able to write good
documentation for others to read.  If you spot any doco that is plainly
wrong or old, please point it out.  If anybody wants to write tutorial
style information about how they learnt to use/configure jetty - that
would be excellent!

Note that Jetty is copyright Mort Bay Consulting and under the 
Artistic Open Source license.  This license does not prevent Mort Bay
from making "other arrangements" including commercial ones for
the non open source use of Jetty.  If you want to avoid your contributions 
being used in this way, please clearing indicate their copyright and 
licensing conditions. 


And remember, it's spelt J-e-t-t-y, but it's pronounced "Jetty".

