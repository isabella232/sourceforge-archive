#
# Makefile for Jetty on Win32
# No multiline recipes to avoid tab/space conversion problems.
#
# REQUIRES: make find rm (Cygwin), jar javac (JDK), zip (Info-Zip)
# Also Perl is required for the "rebuild" or "gen*" rules (only).
#
# This won't work with older Cygwin versions (B19? and older) as they 
# made a major change to how makefiles are interpreted on Win32.
# Lookup the on-line discussion of MAKE_MODE if you want more information.  
# This makefile should work with the default (MAKE_MODE=UNIX).
#
# The only(?) thing Win32 specific about this file is the use of
# the classpath delimiter ';' instead of ':'.
#
# Otherwise this should work as well with Unix.
#

all : build

build : jars-debug jars-release build-demos
rebuild : clean gen build
clean : ; -rm -rf classes lib/*.jar

#
# Generate the source for the "build" classes used to simplify the Java build.
#
# Note that in the "contrib" directory, 
# you need JSSE (and therefore JDK 1.2.1 or later) to build:
#   au.com.forge.Protekt.Jetty.ProtektListener
#   com.kiwiconsulting.jetty.JettyJavaSSLHttpListener
# so they are presently excluded (to allow JDK 1.1.* builds).
#

gen : gen-jetty gen-contrib gen-demos # comment out these dependencies if you don't have Perl
gen-jetty   : ; perl etc/gen-build-class.pl BuildJetty src > src/BuildJetty.java
gen-contrib : ; perl etc/gen-build-class.pl BuildContrib contrib | egrep "[{}]|com\.mortbay|org.gjt|uk.org.gosnell" > contrib/BuildContrib.java
gen-demos   : ; perl etc/gen-build-class.pl BuildDemos   contrib | egrep "[{}]| [A-Za-z0-9]*\.class" > contrib/BuildDemos.java

build-debug   : classes-compile-debug properties-copy-debug
build-release : classes-compile-release properties-copy-release
build-demos   : classes-compile-demos

SOURCES = src/BuildJetty.java contrib/BuildContrib.java

JAVAC_DEBUG   = javac -g -d classes/debug
JAVAC_RELEASE = javac -O -d classes/release
JAVAC_DEMOS   = javac -O -d servlets

classes-mkdir           : ; cd classes || mkdir classes
classes-mkdir-debug     : classes-mkdir ; cd classes/debug || mkdir classes/debug
classes-mkdir-release   : classes-mkdir ; cd classes/release || mkdir classes/release
classes-compile-debug   : classes-mkdir-debug   ; export CLASSPATH="src;contrib;." && $(JAVAC_DEBUG)   $(SOURCES)
classes-compile-release : classes-mkdir-release ; export CLASSPATH="src;contrib;." && $(JAVAC_RELEASE) $(SOURCES)
classes-compile-demos   : ; export CLASSPATH="lib/javax.servlet.jar;contrib;." && $(JAVAC_DEMOS) contrib/BuildDemos.java

properties-copy           : properties-copy-debug properties-copy-release

properties-copy-debug     : properties-zip ; cd classes/debug && unzip -o ../properties
properties-copy-release   : properties-zip ; cd classes/release && unzip -o ../properties
properties-zip            : properties-zip-gnujsp properties-zip-javax
properties-zip-gnujsp     : ; cd contrib && find org/gjt/jsp -name *.properties | zip -@u ../classes/properties
properties-zip-javax      : ; cd src     && find javax -name *.properties | zip -@u ../classes/properties

jars-release              : jar-gnujsp-release jar-javax-servlet-release jar-com-mortbay-release
classes-release           : classes-compile-release properties-copy-release
jar-gnujsp-release        : classes-release ; cd classes/release && jar -cf ../../lib/gnujsp.jar org/gjt/jsp
jar-javax-servlet-release : classes-release ; cd classes/release && jar -cf ../../lib/javax.servlet.jar javax/servlet
jar-com-mortbay-release   : classes-release ; cd classes/release && jar -cf ../../lib/com.mortbay.jar com/mortbay uk/org/gosnell

jars-debug                : jar-gnujsp-debug jar-javax-servlet-debug jar-com-mortbay-debug
classes-debug             : classes-compile-debug properties-copy-debug
jar-gnujsp-debug          : classes-debug ; cd classes/debug && jar -cf ../../lib/debug.gnujsp.jar org/gjt/jsp
jar-javax-servlet-debug   : classes-debug ; cd classes/debug && jar -cf ../../lib/debug.javax.servlet.jar javax/servlet
jar-com-mortbay-debug     : classes-debug ; cd classes/debug && jar -cf ../../lib/debug.com.mortbay.jar com/mortbay uk/org/gosnell
