#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

ifndef JETTY_HOME
    JETTY_HOME = $(shell pwd)
    export JETTY_HOME
endif
ifndef INSTALLROOT
    INSTALLROOT = $(shell pwd)
    export INSTALLROOT
endif

rall:

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := .
SUBDIRS := \
    src \
    contrib/org/gjt/jsp \
    contrib/com/mortbay/HTTP/Handler \
    contrib/com/mortbay/Jetty \
    contrib/uk/org/gosnell/Servlets

include $(JETTY_HOME)/etc/Global.mk


buildJetty:
	echo "class BuildJetty{" > src/BuildJetty.java
	echo "Class[] classes={" >> src/BuildJetty.java
	find src \( -name '*.java' -o -name 'Jetty/etc' -prune \) -print |\
	xargs egrep 'public.*class' | cut -d: -f1 | sort -u |\
	sed -e 's/^src./    /' -e 's/\//./g' -e 's/.java$$/.class,/' >> src/BuildJetty.java
	echo "};}" >> src/BuildJetty.java

buildContrib:
	echo "class BuildContrib{" > contrib/BuildContrib.java
	echo "Class[] classes={" >> contrib/BuildContrib.java
	find contrib -name '*.java' |\
	xargs egrep 'public.*class' | cut -d: -f1 | sort -u |\
	sed -e 's/^contrib./    /' -e 's/\//./g' -e 's/.java$$/.class,/' >> contrib/BuildContrib.java
	echo "};}" >> contrib/BuildContrib.java


#help target  jetty-release    Make a release of Jetty
jetty-release: buildJetty buildContrib jetty-jar javax-jar cleansrc
	mkdir -p lib

javax-jar:
	mkdir -p lib
	cd $(INSTALLROOT)/classes \
	&& jar cfv javax.servlet.jar \
		javax \
	&& mv javax.servlet.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/javax.servlet.jar to your CLASSPATH
	@echo 

jetty-jar:
	mkdir -p lib
	make rall 
	make install
	cd $(INSTALLROOT)/classes \
	&& jar cfv com.mortbay.Jetty.jar \
		com/mortbay/Base \
		com/mortbay/Util \
		com/mortbay/FTP \
		com/mortbay/JDBC \
		com/mortbay/HTML \
		com/mortbay/HTTP \
		com/mortbay/Servlets \
		com/mortbay/Jetty \
	&& mkdir -p ../lib \
	&& mv com.mortbay.Jetty.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/com.mortbay.Jetty.jar to your CLASSPATH
	@echo 

javadoc:
	bin/generateJavaDoc

cleansrc:
	cd src && make rclean

