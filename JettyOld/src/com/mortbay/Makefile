#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

rall:

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := com
SUBDIRS := Base Util FTP JDBC  HTML  HTTP Servlets Jetty 


ifndef JETTY_HOME
  ifdef MORTBAY_HOME
    JETTY_HOME := $(MORTBAY_HOME)
  else
    JETTY_HOME := /usr/local/Jetty
  endif
endif
include $(JETTY_HOME)/etc/Global.mk
