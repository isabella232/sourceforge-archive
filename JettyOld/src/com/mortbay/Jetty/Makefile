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

ifndef MORTBAY_HOME
MORTBAY_HOME := /usr/local/MortBay
endif
include $(MORTBAY_HOME)/etc/Global.mk
