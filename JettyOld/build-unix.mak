#
# Makefile for Jetty
# No multiline recipes to avoid tab/space conversion problems.
#
# REQUIRES: make find rm (Cygwin/GNU), jar javac (JDK), zip (Info-Zip)
# Also Perl is required for the "rebuild" or "gen*" rules (only).
#
# This won't work with older Cygwin versions (B19? and older) as they 
# made a major change to how makefiles are interpreted on Win32.
# Lookup the on-line discussion of MAKE_MODE if you want more information.  
# This makefile should work with the default (MAKE_MODE=UNIX).
#
# The only thing Win32 or Unix specific about this file is the use of
# the classpath delimiter ';' instead of ':'.
#
# Note we use ./classes for temporary *.zip files for easy cleanup.
#

all : build site-build

build : jars-debug jars-release build-demos
rebuild : clean gen build
clean : ; -rm -rf classes FileBase lib/debug lib/release

# Note : could clean out *.prp files from ./etc (but this might cause future problems).

#
# Generate the source for the "build" classes used to simplify the Java build.
#
# Note that in the "contrib" directory, 
# you need JSSE (and therefore JDK 1.2.1 or later) to build:
#   au.com.forge.Protekt.Jetty.ProtektListener
#   com.kiwiconsulting.jetty.JettyJavaSSLHttpListener
# so they are presently excluded (to allow JDK 1.1.* builds).
#
# NOTE : Java 2 needed for the com.mortbay.Servlets.PackageIndex package in BuildIndex.
# NOTE : Java 2 needed for the *Jsse* classes (thus excluded from BuildJetty).
# NOTE : Java 2 needed for the com.mortbay.Util.KeyPairTool class (thus excluded from BuildJetty).
#

gen : gen-jetty gen-ssl gen-contrib gen-index gen-jsp gen-demos # comment out these dependencies if you don't have Perl
gen-jetty   : ; perl etc/gen-build-class.pl BuildJetty   src     | egrep -v "Jsse|KeyPairTool" > src/BuildJetty.java
gen-ssl     : ; perl etc/gen-build-class.pl BuildSSL     src     | egrep "[{}]|Jsse|KeyPairTool" > src/BuildSSL.java
gen-contrib : ; perl etc/gen-build-class.pl BuildContrib contrib | egrep "[{}]|com\.mortbay\.(HTTP|Jetty)|uk.org.gosnell" > contrib/BuildContrib.java
gen-index   : ; perl etc/gen-build-class.pl BuildIndex   contrib | egrep "[{}]|com\.mortbay\.Servlets\.PackageIndex" > contrib/BuildIndex.java
gen-jsp     : ; perl etc/gen-build-class.pl BuildJSP     contrib | egrep "[{}]|org.gjt" > contrib/BuildJSP.java
gen-demos   : ; perl etc/gen-build-class.pl BuildDemos   contrib | egrep "[{}]| [A-Za-z0-9]*\.class" > contrib/BuildDemos.java

build-debug   : classes-compile-debug properties-copy-debug
build-release : classes-compile-release properties-copy-release
build-demos   : servlets-compile-demos

# NOTE : Java 2 dependent classes excluded from build here.
SOURCES = src/BuildJetty.java contrib/BuildContrib.java contrib/BuildJSP.java # contrib/BuildIndex.java src/BuildSSL.java

mkdir-classes             : ; cd classes || mkdir classes
mkdir-classes-debug       : mkdir-classes ; cd classes/debug || mkdir classes/debug
mkdir-classes-release     : mkdir-classes ; cd classes/release || mkdir classes/release
mkdir-servlets            : ; cd servlets || mkdir servlets
mkdir-lib                 : ; cd lib || mkdir lib
mkdir-lib-release         : mkdir-lib ; cd lib/release || mkdir lib/release 
mkdir-lib-debug           : mkdir-lib ; cd lib/debug   || mkdir lib/debug
mkdir-filebase            : ; cd FileBase || mkdir FileBase
mkdir-jsp                 : mkdir-filebase ; cd FileBase/JSP || mkdir FileBase/JSP

X_CLASSPATH               = export CLASSPATH="src:contrib:."
X_CLASSPATH_SERVLETS      = export CLASSPATH="lib/release/javax.servlet.jar:contrib:."
X_CLASSPATH_JETTY         = export CLASSPATH="lib/javax.servlet.jar:lib/com.mortbay.jar:lib/gnujsp.jar:."
X_CLASSPATH_JAVADOC       = export CLASSPATH="src:contrib"

JAVAC_DEBUG   = $(X_CLASSPATH) && javac -g -d classes/debug
JAVAC_RELEASE = $(X_CLASSPATH) && javac -O -d classes/release
JAVAC_DEMOS   = $(X_CLASSPATH_SERVLETS) && javac -O -d servlets
JAVA_JETTY    = $(X_CLASSPATH_JETTY) && java

classes-compile-debug     : mkdir-classes-debug   ; $(JAVAC_DEBUG)   $(SOURCES)
classes-compile-release   : mkdir-classes-release ; $(JAVAC_RELEASE) $(SOURCES)

servlets-compile-demos    : jar-javax-servlet-release mkdir-servlets ; $(JAVAC_DEMOS) contrib/BuildDemos.java

properties-copy           : properties-copy-debug properties-copy-release

properties-copy-ide       : properties-zip ; cd classes && unzip -oq properties
properties-copy-debug     : properties-zip ; cd classes/debug && unzip -oq ../properties
properties-copy-release   : properties-zip ; cd classes/release && unzip -oq ../properties
properties-zip            : properties-zip-gnujsp properties-zip-javax
properties-zip-gnujsp     : ; cd contrib && find org/gjt/jsp -name *.properties | zip -@uq ../classes/properties
properties-zip-javax      : ; cd src     && find javax -name *.properties | zip -@uq ../classes/properties

jars-release              : jar-gnujsp-release jar-javax-servlet-release jar-com-mortbay-release
classes-release           : classes-compile-release properties-copy-release
jar-gnujsp-release        : classes-release mkdir-lib-release ; cd classes/release && jar -cf ../../lib/release/gnujsp.jar org/gjt/jsp
jar-javax-servlet-release : classes-release mkdir-lib-release ; cd classes/release && jar -cf ../../lib/release/javax.servlet.jar javax/servlet
jar-com-mortbay-release   : classes-release mkdir-lib-release ; cd classes/release && jar -cf ../../lib/release/com.mortbay.jar com/mortbay uk/org/gosnell

jars-debug                : jar-gnujsp-debug jar-javax-servlet-debug jar-com-mortbay-debug
classes-debug             : classes-compile-debug properties-copy-debug
jar-gnujsp-debug          : classes-debug mkdir-lib-debug ; cd classes/debug && jar -cf ../../lib/debug/gnujsp.jar org/gjt/jsp
jar-javax-servlet-debug   : classes-debug mkdir-lib-debug ; cd classes/debug && jar -cf ../../lib/debug/javax.servlet.jar javax/servlet
jar-com-mortbay-debug     : classes-debug mkdir-lib-debug ; cd classes/debug && jar -cf ../../lib/debug/com.mortbay.jar com/mortbay uk/org/gosnell

#
# Build a usable "site" in the current directory, for use in testing.
#

site-build                : servlets-compile-demos filebase-copy etc-copy jsp-copy properties-copy-ide

filebase-copy             : filebase-zip ; unzip -oq classes/filebase
filebase-zip              : mkdir-classes ; cd src/com/mortbay/Jetty && find FileBase -type f | egrep -v "/CVS/" | zip -@uq ../../../../classes/filebase 

etc-copy                  : etc-zip ; unzip -oq  classes/etc
etc-zip                   : mkdir-classes ; cd src/com/mortbay/Jetty && find etc -type f | egrep -v "/CVS/" | zip -@uq ../../../../classes/etc

jsp-copy                  : mkdir-jsp ; cp contrib/org/gjt/jsp/jsp/*.jsp FileBase/JSP/.

#
# Build and run the Jetty demo.
#

demo                      : site-build demo-debug
demo-debug                : use-debug   build-demos ; $(JAVA_JETTY) -DDEBUG=1 com.mortbay.Jetty.Demo
demo-release              : use-release build-demos ; $(JAVA_JETTY) com.mortbay.Jetty.Demo
use-debug                 : jars-debug   ; cp lib/debug/*.jar lib/.
use-release               : jars-release ; cp lib/release/*.jar lib/.

#
# How to generate javadoc documentation from sources.
#

PACKAGES = \
com.mortbay.Base \
com.mortbay.FTP \
com.mortbay.HTML \
com.mortbay.HTTP \
com.mortbay.HTTP.Configure \
com.mortbay.HTTP.Filter \
com.mortbay.HTTP.HTML \
com.mortbay.HTTP.Handler \
com.mortbay.JDBC \
com.mortbay.Jetty \
com.mortbay.Servlets \
com.mortbay.Util \
com.mortbay.Util.Test \
javax.servlet \
javax.servlet.http \
javax.servlet.jsp \
org.gjt.jsp \
org.gjt.jsp.jsdk20 \
uk.org.gosnell.Servlets

JAVADOC = $(X_CLASSPATH_JAVADOC) && javadoc

docs : ; $(JAVADOC) -d doc $(PACKAGES)
