#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := com

UUFILES := \
	$(wildcard FileBase/Images/*.gif.uu) 
 
CFGFILES := \
	$(wildcard FileBase/Images/*.gif) \
	$(wildcard FileBase/*.html) \
	$(wildcard FileBase/*/*.html) \
	$(wildcard FileBase/*/*/*.html)  

all: 

ifndef JETTY_HOME
  ifdef MORTBAY_HOME
    JETTY_HOME := $(MORTBAY_HOME)
  else
    JETTY_HOME := /usr/local/Jetty
  endif
endif
include $(JETTY_HOME)/etc/Global.mk
