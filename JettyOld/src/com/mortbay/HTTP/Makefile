#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

all:

rall: all

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS := HttpTests.java
EXCLUDECLASSES := HttpTests.class
ROOTNAME := com
BINFILES := 
CFGFILES := 
SUBDIRS = Configure Handler Filter HTML

# Allow building of the ssl components.
CLASSPATH := ${CLASSPATH}:$(JETTY_HOME)/lib/jcert.jar:$(JETTY_HOME)/lib/jnet.jar:$(JETTY_HOME)/lib/jsse.jar

include $(JETTY_HOME)/etc/Global.mk

