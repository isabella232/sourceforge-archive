#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS := Server.java
ROOTNAME := com

UUFILES := \
	$(wildcard FileBase/Images/*.gif.uu) \
	$(wildcard classes/*.uu)

INSTALLPATH := .
INSTALLFILES := \
	JettyServer.prp \
	etc/RealmDemo.prp \
	etc/RealmOther.prp \
	etc/ParamDemo.prp \
	etc/JettyDemo.prp \
        classes/HelloWorldServlet.class \
	$(wildcard FileBase/Images/*.gif) \
	$(wildcard FileBase/*.txt) \
	$(wildcard FileBase/*/*.txt) \
	$(wildcard FileBase/*/*/*.txt) \
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
