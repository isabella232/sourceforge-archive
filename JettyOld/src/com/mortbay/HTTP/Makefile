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
TESTS := Cookies.java HttpTests.java
ROOTNAME := com
BINFILES := 
CFGFILES := 
SUBDIRS = Configure Handler Filter HTML


ifndef JETTY_HOME
  ifdef MORTBAY_HOME
    JETTY_HOME := $(MORTBAY_HOME)
  else
    JETTY_HOME := /usr/local/Jetty
  endif
endif
include $(JETTY_HOME)/etc/Global.mk

