#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

all:

rall: all

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS := TestHarness.java
EXCLUDECLASSES := \
	TestHarness.class \
	TestRFC2616.class \
	TestRFC2616$$TestConnection.class \
	TestRFC2616$$RedirectHandler.class \
	TestRequest.class \
	Http10TestClient.class \


CLASSFILES := web.dtd $(wildcard *.class)

ROOTNAME := com
BINFILES := 
CFGFILES := 
SUBDIRS = 

INSTALLPATH := etc
INSTALLFILES := \
	web.dtd

include $(JETTY_HOME)/etc/Global.mk

