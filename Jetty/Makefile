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
    src 

include $(JETTY_HOME)/etc/Global.mk

jars: 
	cd classes && \
	jar cfv ../lib/com.mortbay.jetty.jar com org/apache/jasper && \
	jar cfv ../lib/javax.servlet.jar javax
