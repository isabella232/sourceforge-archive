#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := javax

UUFILES := \
	$(wildcard *.uu)

LIBFILES := javax.servlet.jar

all: $(LIBFILES) servlet install

clean:: rm-servlet

ifndef MORTBAY_HOME
MORTBAY_HOME := /usr/local/MortBay
endif
include $(MORTBAY_HOME)/etc/Global.mk

servlet: javax.servlet.jar
	jar xf javax.servlet.jar \
	&& mv javax/servlet . \
	&& rm -fr javax META-INF

rm-servlet:
	rm -fr servlet
