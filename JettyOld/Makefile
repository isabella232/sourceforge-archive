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
jetty-release: jetty-jars javadoc cleansrc

jetty-jars:
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
	@echo 
	@echo Add $(shell pwd)/lib/com.mortbay.Jetty.jar to your CLASSPATH

javadoc:
	bin/generateJavaDoc

cleansrc:
	cd src && make rclean

    
