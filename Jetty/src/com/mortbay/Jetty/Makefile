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

WEBAPPDIRS := \
	$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty \
	$(INSTALLROOT)/webapps/$(WEBAPP)/images	\

WEBAPPCLASSES := \
	$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyServlet.class \
	$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyPage.class \
	$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyPage$$Section.class \
	$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyIndex_en.properties

WEBAPPFILES := \
	$(INSTALLROOT)/webapps/$(WEBAPP)/LICENSE.html \
	$(INSTALLROOT)/webapps/$(WEBAPP)/versions.txt \


install: $(WEBAPPDIRS) $(WEBAPPFILES) $(WEBAPPCLASSES)


$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty:
	mkdir -p $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty

$(INSTALLROOT)/webapps/$(WEBAPP)/images:
	mkdir -p $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty


$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyServlet.class: JettyServlet.class
	cp `basename $@` $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty

$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyPage.class: JettyPage.class
	cp `basename $@` $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty

$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyPage$$Section.class: JettyPage$$Section.class
	cp `basename '$@'` $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty

$(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty/JettyIndex_en.properties:
	cp `basename $@` $(INSTALLROOT)/webapps/$(WEBAPP)/WEB-INF/classes/com/mortbay/Jetty


$(INSTALLROOT)/webapps/$(WEBAPP)/LICENSE.html: $(INSTALLROOT)/LICENSE.html
	cp $(INSTALLROOT)/LICENSE.html $(INSTALLROOT)/webapps/$(WEBAPP)/LICENSE.html

$(INSTALLROOT)/webapps/$(WEBAPP)/versions.txt: $(INSTALLROOT)/VERSION.TXT
	cp $(INSTALLROOT)/VERSION.TXT $(INSTALLROOT)/webapps/$(WEBAPP)/versions.txt



