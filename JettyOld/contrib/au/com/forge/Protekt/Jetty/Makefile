#
# Java makefile.
#
# Copyright (c) 1996 Intelligent Switched Systems, Sydney
# $Id$
#

#JAVAOPTIONS := 
#JAVACOPTIONS :=
TESTS :=
ROOTNAME := au

include $(JETTY_HOME)/etc/Global.mk

ProtektListener.class: ProtektListener.class.uu
	uudecode ProtektListener.class.uu

clean::
	rm -f ProtektListener.class
