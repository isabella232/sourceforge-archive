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

include $(JETTY_HOME)/etc/Global.mk

#help target  jetty-release    Make a release of Jetty
jetty-release: jetty-jar javax-jar javadoc cleansrc
	mkdir -p lib

javax-jar:
	cd $(INSTALLROOT)/classes \
	&& jar cfv javax.servlet.jar \
		javax \
	&& mv javax.servlet.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/javax.servlet.jar to your CLASSPATH
	@echo 

jetty-jar:
	make rall 
	make install
	cd $(INSTALLROOT)/classes \
	&& jar cfv com.mortbay.Jetty.jar \
		com/mortbay/Base \
		com/mortbay/Util \
		com/mortbay/FTP \
		com/mortbay/JDBC \
		com/mortbay/HTML \
		HelloWorldServlet.class \
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

