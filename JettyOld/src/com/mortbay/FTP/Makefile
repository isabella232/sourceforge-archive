#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
ROOTNAME := com
BINFILES := 
CFGFILES := 

TESTS := TestFtp.java

all: 

ifndef JETTY_HOME
  ifdef MORTBAY_HOME
    JETTY_HOME := $(MORTBAY_HOME)
  else
    JETTY_HOME := /usr/local/Jetty
  endif
endif
include $(JETTY_HOME)/etc/Global.mk
