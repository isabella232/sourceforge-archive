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
SUBDIRS := STF


ifndef JETTY_HOME
  ifdef MORTBAY_HOME
    JETTY_HOME := $(MORTBAY_HOME)
  else
    JETTY_HOME := /usr/local/Jetty
  endif
endif
include $(JETTY_HOME)/etc/Global.mk
tests:
	make -C Test all
