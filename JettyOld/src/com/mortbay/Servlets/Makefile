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

ifndef MORTBAY_HOME
MORTBAY_HOME := /usr/local/MortBay
endif
include $(MORTBAY_HOME)/etc/Global.mk
