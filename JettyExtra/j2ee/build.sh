#!/bin/sh

JETTY_HOME=$HOME/cvs/jetty/HEAD

#JBOSS_HOME=$HOME/cvs/JBoss/jboss-3.0/build/output/jboss-3.0.5RC1
#JBOSS_HOME=$HOME/cvs/JBoss/jboss-3.2/build/output/jboss-3.2.0beta2
#JBOSS_HOME=$HOME/cvs/JBoss/jboss-3.2/build/output/jboss-3.2.0RC1
JBOSS_HOME=$HOME/cvs/JBoss/jboss-3.2/build/output/jboss-3.2.0RC1

CLASSPATH=
CLASSPATH=$JBOSS_HOME/client/log4j.jar:$CLASSPATH
CLASSPATH=$JBOSS_HOME/client/jbossjmx-ant.jar:$CLASSPATH
CLASSPATH=$JBOSS_HOME/client/jbossall-client.jar:$CLASSPATH
CLASSPATH=$JBOSS_HOME/client/gnu-regexp.jar:$CLASSPATH
export CLASSPATH

exec ant -buildfile build-new.xml -Djboss.home $JBOSS_HOME -Djetty.home $JETTY_HOME $@