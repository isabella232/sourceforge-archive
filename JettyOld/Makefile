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
ROOTNAME := .

ifndef MORTBAY_HOME
MORTBAY_HOME := /usr/local/MortBay
endif
include $(MORTBAY_HOME)/etc/Global.mk

#help target  util-release    Make a release of the mortbay utility packages
util-release:
	@echo Making com.mortbay.jar from Base,Util,FTP,JDBC,HTML
	cd src/com/mortbay \
	&& make -C Base rall \
	&& make -C Util rall \
	&& make -C FTP rall \
	&& make -C JDBC rall \
	&& make -C HTML rall \
	&& make -C Base install \
	&& make -C Util install \
	&& make -C FTP install \
	&& make -C JDBC install \
	&& make -C HTML install \
	&& make -C Applets install
	cd $(INSTALLROOT)/classes \
	&& jar cfv com.mortbay.jar \
		com/mortbay/Base \
		com/mortbay/Util \
		com/mortbay/FTP \
		com/mortbay/JDBC \
		com/mortbay/HTML \
	&& mkdir -p ../lib \
	&& mv com.mortbay.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/com.mortbay.jar to your CLASSPATH

#help target  jetty-release    Make a release of the mortbay utility packages
jetty-release:
	@echo Making com.mortbay.Jetty.jar from HTTP,Servlets,Jetty
	cd src/com/mortbay \
	&& make -C HTTP rall \
	&& make -C Servlets rall \
	&& make -C Jetty rall \
	&& make -C HTTP install \
	&& make -C Servlets install \
	&& make -C Jetty install 
	cd $(INSTALLROOT)/classes \
	&& jar cfv com.mortbay.Jetty.jar \
		HelloWorldServlet.class \
		com/mortbay/HTTP \
		com/mortbay/Servlets \
		com/mortbay/Jetty \
	&& mkdir -p ../lib \
	&& mv com.mortbay.Jetty.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/com.mortbay.Jetty.jar to your CLASSPATH

#help target  accounts-release    Make a release of the accounts packages
accounts-release:
	@echo Making com.mortbay.Accounts.jar from Accounts
	cd src/com/mortbay \
	&& make -C Accounts rall \
	&& make -C Accounts install 
	cd $(INSTALLROOT)/classes \
	&& jar cfv com.mortbay.Accounts.jar \
		com/mortbay/Accounts \
	&& mkdir -p ../lib \
	&& mv com.mortbay.Accounts.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/com.mortbay.Accounts.jar to your CLASSPATH

#help target  tracker-release    Make a release of the tracker packages
tracker-release:
	@echo Making com.mortbay.Tracker.jar from Tracker
	cd src/com/mortbay \
	&& make -C Tracker rall \
	&& make -C Tracker install 
	cd $(INSTALLROOT)/classes \
	&& jar cfv com.mortbay.Tracker.jar \
		com/mortbay/Tracker \
	&& mkdir -p ../lib \
	&& mv com.mortbay.Tracker.jar ../lib
	@echo 
	@echo Add $(shell pwd)/lib/com.mortbay.Tracker.jar to your CLASSPATH


javadoc:
	bin/generateJavaDoc



