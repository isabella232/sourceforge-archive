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
EXCLUDECLASSES := JettyServlet.class JettyPage.class JettyPage$$Section.class

include $(JETTY_HOME)/etc/Global.mk

WEBAPP := jetty
WEBAPPCLASSES := \
	JettyServlet.class \
	JettyPage.class \
	JettyPage?Section.class \
	JettyIndex_en.properties

install: JettyServlet.class JettyPage.class
	mkdir -p $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty
	mkdir -p $(INSTALLROOT)/webapps/$(WEBAPP)/images
	for FILE in $(WEBAPPCLASSES) ; \
	do \
		cp $$FILE $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty ;\
	done







