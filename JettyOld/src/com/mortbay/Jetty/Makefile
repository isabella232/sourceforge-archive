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
	$(wildcard servlets/*.uu)

INSTALLPATH := .
INSTALLFILES := \
	$(wildcard etc/*.prp) \
	$(wildcard etc/*.url) \
        $(wildcard servlets/*.class) \
	$(wildcard FileBase/Images/*.gif) \
	$(wildcard FileBase/*.txt) \
	$(wildcard FileBase/*/*.txt) \
	$(wildcard FileBase/*/*/*.txt) \
	$(wildcard FileBase/*.html) \
	$(wildcard FileBase/*/*.html) \
	$(wildcard FileBase/*/*/*.html)  

all: 

include $(JETTY_HOME)/etc/Global.mk
