#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := org
EXCLUDECLASSES :=  $(wildcard *.class )
CLASSPATH := ../../../../src
SUBDIRS := jsdk20

include $(JETTY_HOME)/etc/Global.mk

install: gnujsp.jar
	mkdir -p $(INSTALLROOT)/FileBase/JSP
	mkdir -p $(INSTALLROOT)/etc/jsp
	cp -fR jsp/*.jsp $(INSTALLROOT)/FileBase/JSP
	cp gnujsp.jar $(INSTALLROOT)/lib

gnujsp.jar: all
	cd ../../.. && jar \
	cf org/gjt/jsp/gnujsp.jar \
	org/gjt/jsp/*.class \
	org/gjt/jsp/jsdk20/*.class \
	org/gjt/jsp/prop/*.properties

clean::
	rm -f gnujsp.jar


