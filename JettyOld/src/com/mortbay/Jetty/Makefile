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
	etc/NSRoamDemo.prp \
        classes/HelloWorldServlet.class \
	$(wildcard FileBase/Images/*.gif) \
	$(wildcard FileBase/*.txt) \
	$(wildcard FileBase/*/*.txt) \
	$(wildcard FileBase/*/*/*.txt) \
	$(wildcard FileBase/*.html) \
	$(wildcard FileBase/*/*.html) \
	$(wildcard FileBase/*/*/*.html)  

all: 

include $(JETTY_HOME)/etc/Global.mk
