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
the $JETTY_HOME/lib directory  e.g.

   Unix:
	CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/javax.servlet.jar
        CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/com.mortbay.Jetty.jar
        CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/gnujsp.jar


The jsse.jar, jnet.jar and jcert.jar are only required if you
intend to run or compile the SSL classes.


CONFIGURATION
=============

Jetty configuration can be considered on two levels. As it is 
intended to be embedded in other applications, the primary
configuration method is to provide an implementation of
the interface com.mortbay.HTTP.HttpConfiguration.

For convenience, several implementations of HttpConfiguration
are supplied with the release:

  com.mortbay.Jetty.Server
    Reads configuration information from java properties files.

  com.mortbay.Jetty.Demo
    A specialization of com.mortbay.Jetty.Server that runs the
    demonstration server(s) for Jetty.

  com.mortbay.Jetty.Server21 
    An old version of Server that works with configuration files 
    for versions prior to 2.2.x

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
    A simple configuration file for files, servlets and JSP.JSJSP

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
    facilities of PropertyTree to abbreviate to a minimal file.
    [ Not included yet ]

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
the directory that contains the class file hierarchy (i.e. the parent 
directory of mypackage/myservlet.class ). Run the server with:

    java com.mortbay.HTTP.Configure.ServletServer 8080 / .

Access the server with a URL like
   
    http://hostname:8080/packagename.classname/pathInfo


If the servlet is in a jar file, run the server with

    java com.mortbay.HTTP.Configure.ServletServer 8080 / myclasses.jar

The server can be run with a search path for directories and jar
files from which servlets are loaded. Note that the path syntax
is platform dependent and the unix for is used here:

    java com.mortbay.HTTP.Configure.ServletServer 8080 / .:myclasses.jar


Access the servlet at:

    http://hostname:8080/servlet/mypackage.myservlet


Loading and reloading of servlets can be controlled by accessing

    http://hostname:8080/LoaderServlet



Running a simple File Server
----------------------------
The com.mortbay.HTTP.Configure.FileServer class runs a file
server.  Help for this class can be obtained by running

   java com.mortbay.HTTP.Configure.FileServer -help

To run to serve files in the current directory:

   java com.mortbay.HTTP.Configure.FileServer 8080 

To run to serve files in another directory:

   java com.mortbay.HTTP.Configure.FileServer 8080 /other/directory



FREQUENTLY ASKED QUESTIONS
==========================

Does Jetty support SSL?
-----------------------
Jetty contains several listeners that support SSL.
In the contrib directories there is a listener for the Protekt SSL 
commercial product from www.forge.com.au and a prototype JSSE
listener from kiwiconsulting.com.

These contributed SSL listeners were used as the basis for the 
supported SSL listeners  com.mortbay.HTTP.JsseListener and
com.mortbay.HTTP.SunJsseListener.   Instructions for SSL are
in $JETTY_HOME/doc/JsseSSL.html


Does Jetty support "standard" server side includes?
---------------------------------------------------
No. Jetty's server include tags are not standard, as they
pre-date the "standard" and use a very simple/fast parser.
We are waiting to see what happens with JSP,SML,etc. before
investing effort towards this.  Standard SSI is available
as a servlet from http://java.apache.org/jservssi/index.html


Does Jetty support chained servlets?
------------------------------------
No. Chained servlets are a brain dead idea, as the servlet/request API
is not a filter style of API. Jetty provides filters which can be used
to modify the output of a servlet/file/forward/proxy request.
Jetty also supports the standard resource API that allows a servlet to
request the content from a URL. This works OK for accessing other
servlets and files.


Can I use Jetty Commercially?
-----------------------------
Yes. To paraphrase the Jetty Open Source license, you can commercially
use and distribute Jetty so long as you:
  + Only use it internally
  + Give Jetty increased distribution by including a full release.
  + Give Jetty increased publicity by including a reference to the
    full release in a public/visible area of the product.
    The "Powered by Jetty", button is a good way to achieve this.
  + Make other arrangements with the copyright holder(s). ie. buy a 
    source license. 

At some time in the future, it is likely that new versions of Jetty will
be released under the apache license (http://www.apache.org), which also
allows commmercial usage.


Why is Jetty Open Source?
-------------------------
Mort Bay Consulting originally wrote Jetty as a java learning excercise
and as a demonstration platform for WWW based office automation. Since
that time it has proved it usefulness in many of the projects that 
Mort Bay has provided consulting for and to date there is no other
server that serves the embedded server niche as well.   

Mort Bay is primarily a consulting company and do not have the resources 
to provide a quality commercial software product. Thus we prefer that
Jetty is openly used and developed, rather than be an "also ran" commercial
product.  The quality of contributions, refinements and bug fixes that
we have received from the Jetty user base has allowed Jetty to increase
in quality of functionality, thus allowing us all to get on with using
it to develop better and more intersting WWW applications for our clients.


Can Jetty listen on a privileged port
--------------------------------------
On many operating systems, IP ports below 1024 need special privileges
to be opened for listening. The standard HTTP port, 80, is such a port.
Under Unix, a frequent solution for WWW servers is to run as superuser
(root) and to change to a safer user ID only after the ports have been
opened.   Jetty supports this mode of operation with a simple 
native method call, that allows Jetty running on Unix systems to 
change to a configured effective user ID after the server has started.  
To run with this option configured, the $JETTY_HOME/lib/<machine>-<system>
directory must be included in the LD_LIBRARY_PATH or equivalent:

  LD_LIBRARY_PATH=$JETTY_HOME/lib/i686-Linux java com.mortbay.Jetty.Server

The distribution comes with the native library built for i686-Linux.
To build the library for your platform, run the following commands:

  cd $JETTY_HOME/src/com/mortbay/Jetty
  make native
  make install

Note that this will only work for Unix systems, but the approach 
should be able to be adapted to other operating systems.


Does Jetty support CGI?
-----------------------
There is a CGI servlet in the contrib/uk/org/gosnell/Servlet
directory.  It is configured into the demo as part of the 
dump demonstration.


MISCELLANEOUS
=============

Servlet Loading
---------------
Each servlet (and the classes it uses) is loaded in it's own ClassLoader
instance. The class loader uses the following search strategy when 
looking for classes:

 1) If the class is in the java.* javax.*  package hierarchy, it is loaded 
    from the JVM loader. If it is not found, then the search does not 
    continue.

 2) The class path configured for the servlet is searched for the
    class. The path may contain directories or jar files.

 3) If the class has not been found, the JVM loader is tried.
    Classes loaded from here cannot by dynamically reloaded.

Servlet reloading is implemented by discarding the servlets classloader
and all classes loaded by it. If a servlet instantiated a Thread, it
will continue to run on the original version of the class(es) until it
is restarted. It is the servlet implementors job to control the life
cycle of any such threads.


Servlet Reloading
-----------------
The servlet Holder class has a reload method and an auto reload state.
The reload methods waits 5 seconds for all requests to the servlet to
complete, before calling destroy on all servlets and creating a new
ServletLoader for future requests.

The auto reload method is an expensive check on each request to the 
servlet. It checks the modification date of any class or jar file used
to load any class for the servlet. If there has been a modification, then
the servlet (and all it's referenced classes) are reloaded before the 
request is handled.   This mode can be configured in the Server configuration
file for all dynamically loaded servlets - which is an excellent servlet
development mode.


HTTP/1.1
--------
Servlets by default will only use persistent connections if
the content length is set or the chunked transfer encoding has
been set in the response header.  However, chunking can be used
by default, which allows persistent connections to be used in more 
situations. Please see the ServletHandler class for information
on turning Chunking on by default.

Jetty 2 supports HTTP/1.1 as defined in RFC2068.  This has now been
replaced with RFC2616 and Jetty 3 is currently under development with 
to support this.


Running the Jetty Demo under MacOS
----------------------------------
Running Jetty under MacOS requires MacOS Runtime for Java and the MRJ SDK, both
available from http://developer.apple.com/java/. 

Use JBindery to make a double-clickable application to run the Jetty demo:

- Launch JBindery.
- In the Class name field, enter "com.mortbay.Jetty.Demo"
- Click on the Classpath icon to show the Classpath screen.
- Drag and drop the lib files (com.mortbay.Jetty.jar, gnujsp.jar,
javax.servlet.jar) to the "Additions to class path" box.
- Click Save Settings. Save the settings as an application in the Jetty 2.2
folder (the same folder that contains the etc and FileBase folders).
- Double-click the application you just created to start the server.


Virtual Hosts
-------------
The handler com.mortbay.HTTP.Handler.VirtualHostHandler handles 
virtual hosts by performing a path prefix translation. This is not
a complete solution but works in many cases.


Building Jetty
--------------
Jetty is built using recursive GNU make files, which should be available
on most platforms.  Help on the makefile setup can be obtained by with the
command:
  make help

Not that some build rules use Unix commands which are not portable.  A 
portable make file is in progress, but until that time the file 
src/BuildJetty.java is generated with a link to all Jetty classes.
Thus JDK's dependancy checking can be used to compile Jetty with the
commands:

  cd $JETTY_HOME/src
  javac BuildJetty.java


There is a similar BuildContrib.java file for the contributed source
in the contrib directory.

The build-win32.mak file is a GNU makefile for windows platforms that
uses the cygnus utilities to build Jetty from BuildJetty.java.


COMMON PROBLEMS
===============

Cannot read property file
-------------------------
Some JVMs appear to have problems reading property files that
mix their line termination characters.  The files in this release
have been created under Unix, so are terminated with LF. If you edit
these files under another OS, make sure you have a consistent line
termination character (CRLF for DOS & Windows, LF for Unix, CR for Mac).

GNUJSP locks or fails during compile
------------------------------------
The default GNUJSP configuration uses a command line compiler. Try
changing the compiler property for the gnujsp servlet to some of the
options suggested in the GNUJSP INSTALL file.  Also check the
arguments defined for the JSP compiler, specifically the path to the
gnujsp.jar.  If all else fails, put gnujsp into the system CLASSPATH.

CLASSPATH does not work
-----------------------
Check that you use ':' or ';' as appropriate for your platform
when separating directories and jar files in CLASSPATHS.

PATHS does not work in config file
----------------------------------
For URL path lists, ';' or ',' should be used as a separator.

Method not found exception
--------------------------
An earlier version of the javax.servlet package may be in your
CLASSPATH.



CONTACTS
========
We are experimenting with the public mail list service www.egroups.com
to maintain the Jetty mailing lists and archives. Please report any
problems you have with this service to the general mortbay contact listed
below.

    mortbay@mortbay.com        - for general contact

    jetty-support@egroups.com  - For problems with Jetty
                               - Archives at
                                 http://www.egroups.com/group/jetty-support
    jetty@mortbay.com          - For jetty support questions that are
                                 not stored in the public archive.

    jetty-announce@egroups.com - For Jetty related announcements
                                 This is for announcements of interest
                                 to all Jetty users. It is not for
                                 support questions.
		               - Archives at
                                 http://www.egroups.com/group/jetty-announce
    jetty-announce-subscribe@egroups.com   
                               - join the announce mailing list

    jetty-discuss@egroups.com  - For Jetty related discussions
                                 This is for announcements of interest
                                 to Jetty developers. 
		               - Archives at
                                 http://www.egroups.com/group/jetty-discuss
    jetty-discuss-subscribe@egroups.com   
                               - join the discuss mailing list

    jetty3-discuss@egroups.com - For discussions about the Jetty3/Jakarta 
                                 project.  This is for announcements of 
                                 interest to Jetty developers. 
		               - Archives at
                                 http://www.egroups.com/group/jetty3-discuss
    jetty3-discuss-subscribe@egroups.com   
                               - join the discuss 3 mailing list



SUPPORT
=======
Mort Bay provides email support for Jetty. Informal support is 
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
