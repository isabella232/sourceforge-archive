# Building Netscape Plugins

# These variables may need to be changed from version to version of the
# netscape liveconnect sdk
NSVERSION := 30b5
NSPLATFORM := $(shell uname -s)$(shell uname -r)
NSCLASSES := moz3_01.zip

LIVECONNECT := /usr/local/src/PluginSDK$(NSVERSION)
CFLAGS := -DXP_UNIX -I$(LIVECONNECT)/include -I$(LIVECONNECT) $(CFLAGS) \
	  -I/usr/openwin/include
JCLASSPATH :=$(CLASSPATH):$(LIVECONNECT)/classes/$(NSCLASSES):$(JDK_HOME)/lib/classes.zip
JAVAC := $(JAVAC) -classpath $(JCLASSPATH)
JAVAH := $(LIVECONNECT)/bin/$(NSPLATFORM)/javah -classpath $(JCLASSPATH) -jri
