#!/bin/sh

#
# Startup script for jetty under *nix systems (it works under NT/cygwin too).
#
# Configuration files
#
# $HOME/.jettyrc
#   This is read at the start of script. It may perform any sequence of
#   shell commands, like setting relevant environment variables.
#
# /etc/jetty.conf
#   If found, it will be used as this script's configuration file. Each
#   line of the file may contain:
#     - A comment denoted by the pound (#) sign as first non-blank character.
#     - The path to a regular file, which will be passed to jetty as a 
#       server.xml file.
#     - The path to a directory. Each *.xml file in the directory will be
#       passed to jetty as a server.xml file.
#
#   The files will be checked for existence before being passed to jetty.
#
# $JETTY_HOME/etc/jetty.conf
#   If found, used as this script's configuration file, but only if
#   /etc/jetty.conf was not present. See above.
#   
# 
# Configuration variables
#
# JAVA_HOME  
#   Home of Java installation. This needs to be set as this script 
#   will not look for it.
#
# JAVACMD
#   Command to invoke Java. If not set, $JAVA_HOME/bin/java will be
#   used.
#
# JETTY_HOME 
#   Where Jetty is installed. If not set, the script will try go
#   guess it by first looking at the invocation path for the script,
#   and then by looking in standard locations as $HOME/opt/jetty
#   and /opt/jetty. The java system property "jetty.home" will be
#   set to this value for use by configure.xml files, f.e:
#
#    <Arg><SystemProperty name="jetty.home" default="."/>/webapps/jetty.war</Arg>
#
# JETTY_LOGS 
#   Where jetty logs should be stored. The only effect of this 
#   variable is to set the "jetty.logs" java system property so
#   configure.xml files can use it, f.e.:
#
#     <Arg><SystemProperty name="jetty.logs" default="./logs"/>/yyyy_mm_dd.request.log</Arg>
#
#   This variable will be tipically set to something like /var/log/jetty.
#

# see if there's a user-specific configuration file
if [ -f $HOME/.jettyrc ] ; then 
  . $HOME/.jettyrc
fi

# jetty's hallmark
JETTY_JAR="lib/com.mortbay.jetty.jar"

# try to determine JETTY_HOME if not set
if [ -z $JETTY_HOME ] ;
then
  JETTY_HOME_1=`dirname "$0"`/..
  echo "Guessing JETTY_HOME from jetty.sh to ${JETTY_HOME_1}" 
  if [ -f "${JETTY_HOME_1}/${JETTY_JAR}" ] ; 
  then 
     JETTY_HOME=${JETTY_HOME_1} 
     echo "Setting JETTY_HOME to $JETTY_HOME" ;
  fi
fi

# try to find jetty in standard locations
if [ "$JETTY_HOME" = "" ] ; then
  if [ -f ${HOME}/opt/jetty/${JETTY_JAR} ] ; then 
    JETTY_HOME=${HOME}/opt/jetty
    echo "Defaulting JETTY_HOME to $JETTY_HOME"
  fi

  if [ -f /opt/jetty/${JETTY_JAR} ] ; then 
    JETTY_HOME=/opt/jetty
    echo "Defaulting JETTY_HOME to $JETTY_HOME"
  fi
 
  # Add other "standard" locations for jetty
fi

if [ "$JETTY_HOME" = "" ] ; then
    echo JETTY_HOME not set, you need to set it or install in a standard location
    exit 1
fi

if [ -z $JETTY_CONF ] ;
then
  if [ -f /etc/jetty.conf ];
  then
     JETTY_CONF=/etc/jetty.conf;
  else
     JETTY_CONF="${JETTY_HOME}/etc/jetty.conf;"
  fi
fi

# read the configuration file if one exists
if [ -f $JETTY_CONF ] && [ -r $JETTY_CONF ] ;
then
  SERVER_LINES=`cat $JETTY_CONF | grep -v "^[:space:]*#" | tr "\n" " "` ;
fi

# get the list of server.xml files
SERVERS=""
if [ ! -z "${SERVER_LINES}" ] ;
then
  for SERV in ${SERVER_LINES} ;
  do
    if [ ! -r "$SERV" ] ;
    then
      echo "Cannot read '$SERV' specified in '$JETTY_CONF'" ;
    elif [ -f "$SERV" ] ;
    then
      # assume it's a configure.xml file
      SERVERS="$SERVERS $SERV" ;
    elif [ -d "$SERV" ] ;
    then
      # assume it's a directory with configure.xml files
      # for example: /etc/jetty.d/
      # sort the files before adding them to the list of servers
      XML_FILES=`ls ${SERV}/*.xml | sort | tr "\n" " "` 
      for CONF in ${XML_FILES} ;
      do
         if [ -r "$CONF" ] && [ -f "$CONF" ] ;
         then
            SERVERS="$SERVERS $CONF" ;
         else
           echo "Cannot find '$CONF' specified in '$JETTY_CONF'" ;
         fi
      done
    else
      echo "Don''t know what to do with '$SERV' specified in '$JETTY_CONF'" ;
    fi
  done
fi

# Run the demo if there's nothing els to run
if [ -z "${SERVERS}" ] ;
then
  SERVERS="${JETTY_HOME}/etc/demo.xml";
fi


# Check if the admin servlet was added. Add it if not. It will be needed
# to stop jetty cleanly.
HAS_ADMIN=`cat $SERVERS | grep "admin.xml"`
if [ -z "$HAS_ADMIN" ]  ;
then
  SERVERS="${SERVERS} ${JETTY_HOME}/etc/admin.xml"
fi

# Check where logs should go.
if [ -z "$JETTY_LOGS" ] ;
then
  JETTY_LOGS="${JETTY_HOME}/logs"
fi

# Are we running on Windows? Could be, with Cygwin/NT.
if [ -z "$WINBOOTDIR" ] ;
then
  PATH_SEPARATOR=":" ;
else
  PATH_SEPARATOR=";" ;
fi

if [ -z "$JAVACMD" ] ;
then
  JAVACMD="$JAVA_HOME/bin/java" ;
fi

# Build the classpath with Jetty's bundled libraries.
CP=`ls $JETTY_HOME/lib/*.jar | tr "\n" "$PATH_SEPARATOR"`
JVMARGS="-Djetty.home=$JETTY_HOME -Djetty.logs=$JETTY_LOGS $JVMARGS"

# Comment these out after you're happy with what the script is doing.
echo JETTY_HOME=$JETTY_HOME
echo JETTY_CONF=$JETTY_CONF
echo JETTY_LOGS=$JETTY_LOGS
echo SERVERS=$SERVERS
echo PATH_SEPARATOR=$PATH_SEPARATOR
echo JVMARGS=$JVMARGS
echo JAVACMD=$JAVACMD
echo CLASSPATH=$CP


RUNCMD="$JAVACMD -cp "$CP" $JVMARGS com.mortbay.Jetty.Server $SERVERS"

# Run jetty
if [ "$1" = "start" ] ;
then
  ${RUNCMD} &
else
  $RUNCMD ;
fi
