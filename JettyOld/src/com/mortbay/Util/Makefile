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

ifndef MORTBAY_HOME
MORTBAY_HOME := /usr/local/MortBay
endif
include $(MORTBAY_HOME)/etc/Global.mk

tests:
	make -C Test all
