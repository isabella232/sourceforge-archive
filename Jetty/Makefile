#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

ifndef JETTY_HOME
    JETTY_HOME = $(shell pwd)
    export JETTY_HOME
endif
ifndef INSTALLROOT
    INSTALLROOT = $(shell pwd)
    export INSTALLROOT
endif

rall:

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := .
SUBDIRS := \
    servlet2.1/javax \
    src 

include $(JETTY_HOME)/etc/Global.mk

jdk2: 
	bin/useJava2Collections
