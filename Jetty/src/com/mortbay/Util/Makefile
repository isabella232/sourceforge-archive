#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS := TestHarness.java
ROOTNAME := com
SUBDIRS := 
INSTALLPATH := etc
INSTALLFILES := \
	configure.dtd \

EXCLUDECLASSES := \
	TestHarness.class \
	TestHarness$$1.class \
	TestHarness$$2.class \
	TestHarness$$TestThreadedServer.class \
	TestHarness$$TestThreadPool.class \
	TestConfiguration.class \


include $(JETTY_HOME)/etc/Global.mk

install: $(INSTALLROOT)/classes/com/mortbay/Util/configure.dtd

$(INSTALLROOT)/classes/com/mortbay/Util/configure.dtd: configure.dtd
	[ -d $(INSTALLROOT)/classes/com/mortbay/Util ] || mkdir -p $(INSTALLROOT)/classes/com/mortbay/Util
	cp configure.dtd $(INSTALLROOT)/classes/com/mortbay/Util/configure.dtd
