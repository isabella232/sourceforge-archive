#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := javax

UUFILES := \
	$(wildcard *.uu)

LIBFILES := javax.servlet.jar

rall: all

all: $(LIBFILES) servlet install

clean:: rm-servlet


ifndef JETTY_HOME
  ifdef MORTBAY_HOME
    JETTY_HOME := $(MORTBAY_HOME)
  else
    JETTY_HOME := /usr/local/Jetty
  endif
endif
include $(JETTY_HOME)/etc/Global.mk
servlet: javax.servlet.jar
	jar xf javax.servlet.jar \
	&& mv javax/servlet . \
	&& rm -fr javax META-INF

rm-servlet:
	rm -fr servlet
