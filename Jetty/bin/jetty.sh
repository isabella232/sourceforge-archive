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
#   If found, and no configurations were given on the command line,
#   the file will be used as this script's configuration. 
#   Each line in the file may contain:
#     - A comment denoted by the pound (#) sign as first non-blank character.
#     - The path to a regular file, which will be passed to jetty as a 
#       config.xml file.
#     - The path to a directory. Each *.xml file in the directory will be
#       passed to jetty as a config.xml file.
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
# JAVA
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
# JETTY_LOG 
#   Where jetty logs should be stored. The only effect of this 
#   variable is to set the "jetty.log" java system property so
#   configure.xml files can use it, f.e.:
#
#     <Arg><SystemProperty name="jetty.log" default="./logs"/>/yyyy_mm_dd.request.log</Arg>
#
#   This variable will be tipically set to something like /var/log/jetty. If
#   not set, it will default to $JETTY_HOME/logs
#
# JETTY_RUN
#   Where the jetty.pid file should be stored. It defaults to the
#   first available of /var/run, /usr/var/run, and /tmp if not set.
#   

TMPJ=/tmp/j$$

##################################################
# Get the action & configs
##################################################
ACTION=$1
shift
ARGS="$*"
CONFIGS=""

##################################################
# Find directory function
##################################################
findDirectory()
{
    OP=$1
    shift
    for L in $* ; do
        [ $OP $L ] || continue 
        echo $L
        break
    done 
}


##################################################
# See if there's a user-specific configuration file
##################################################
if [ -f $HOME/.jettyrc ] ; then 
  . $HOME/.jettyrc
fi


##################################################
# Jetty's hallmark
##################################################
JETTY_JAR="lib/com.mortbay.jetty.jar"


##################################################
# Try to determine JETTY_HOME if not set
##################################################
if [ -z $JETTY_HOME ] 
then
  JETTY_HOME_1=`dirname "$0"`
  JETTY_HOME_1=`dirname "$JETTY_HOME_1"`
  if [ -f "${JETTY_HOME_1}/${JETTY_JAR}" ] ; 
  then 
     JETTY_HOME=${JETTY_HOME_1} 
  fi
fi


##################################################
# if no JETTY_HOME, search likely locations.
##################################################
if [ "$JETTY_HOME" = "" ] ; then
  STANDARD_LOCATIONS="           \
        $HOME                    \
        ${HOME}/opt/             \
        /opt                     \
        /usr/share               \
        /usr/share/java          \
        /usr/local               \
        /usr/local/share         \
        /usr/local/share/java    \
        /home                    \
        "
  JETTY_DIR_NAMES="              \
        jetty                    \
        Jetty                    \
        jetty3                   \
        Jetty3                   \
        "
        
  JETTY_HOME=
  for L in $STANDARD_LOCATIONS 
  do
     for N in $JETTY_DIR_NAMES 
     do
         if [ -d "$L/$N" ] && [ -f "$L/${N}/${JETTY_JAR}" ] ; 
         then 
            JETTY_HOME="$L/$N"
            echo "Defaulting JETTY_HOME to $JETTY_HOME"
            break
         fi
     done
     [ ! -z "$JETTY_HOME" ] && break
  done
fi

##################################################
# No JETTY_HOME yet? We're out of luck!
##################################################
if [ -z "$JETTY_HOME" ] ; then
    echo "** ERROR: JETTY_HOME not set, you need to set it or install in a standard location" 
    exit 1
else
    if [ ! -r $JETTY_HOME/$JETTY_JAR ] 
    then
       echo "** ERROR: Oops! $JETTY_HOME/$JETTY_JAR is not readable!" 
       exit 1
    fi
fi

###########################################################
# Get the list of config.xml files from the command line.
###########################################################
if [ ! -z "$ARGS" ] 
then
  for A in $ARGS 
  do
    if [ -f $A ] 
    then
       CONF="$A" 
    elif [ -f $JETTY_HOME/etc/$A ] 
    then
       CONF="$JETTY_HOME/etc/$A" 
    elif [ -f ${A}.xml ] 
    then
       CONF="${A}.xml" 
    elif [ -f $JETTY_HOME/etc/${A}.xml ] 
    then
       CONF="$JETTY_HOME/etc/${A}.xml" 
    else
       echo "** ERROR: Cannot find configuration '$A' specified in the command line." 
       exit 1
    fi
    if [ ! -r $CONF ] 
    then
       echo "** ERROR: Cannot read configuration '$A' specified in the command line." 
       exit 1
    fi
    CONFIGS="$CONFIGS $CONF"
  done
fi


##################################################
# Try to find this script's configuration file,
# but only if no configurations were given on the
# command line.
##################################################
if [ -z "$JETTY_CONF" ] 
then
  if [ -f /etc/jetty.conf ]
  then
     JETTY_CONF=/etc/jetty.conf
  else
     JETTY_CONF="${JETTY_HOME}/etc/jetty.conf"
  fi
fi

##################################################
# Read the configuration file if one exists
##################################################
CONFIG_LINES=
if [ -z "$CONFIGS" ] && [ -f "$JETTY_CONF" ] && [ -r "$JETTY_CONF" ] 
then
  CONFIG_LINES=`cat $JETTY_CONF | grep -v "^[:space:]*#" | tr "\n" " "` 
fi

##################################################
# Get the list of config.xml files from jetty.conf
##################################################
if [ ! -z "${CONFIG_LINES}" ] 
then
  for CONF in ${CONFIG_LINES} 
  do
    if [ ! -r "$CONF" ] 
    then
      echo "** WARNING: Cannot read '$CONF' specified in '$JETTY_CONF'" 
    elif [ -f "$CONF" ] 
    then
      # assume it's a configure.xml file
      CONFIGS="$CONFIGS $CONF" 
    elif [ -d "$CONF" ] 
    then
      # assume it's a directory with configure.xml files
      # for example: /etc/jetty.d/
      # sort the files before adding them to the list of CONFIGS
      XML_FILES=`ls ${CONF}/*.xml | sort | tr "\n" " "` 
      for FILE in ${XML_FILES} 
      do
         if [ -r "$FILE" ] && [ -f "$FILE" ] 
         then
            CONFIGS="$CONFIGS $FILE" 
         else
           echo "** WARNING: Cannot read '$FILE' specified in '$JETTY_CONF'" 
         fi
      done
    else
      echo "** WARNING: Don''t know what to do with '$CONF' specified in '$JETTY_CONF'" 
    fi
  done
fi

#####################################################
# Run the demo server if there's nothing else to run
#####################################################
if [ -z "$CONFIGS" ] 
then
  CONFIGS="${JETTY_HOME}/etc/demo.xml ${JETTY_HOME}/etc/admin.xml"
fi


#####################################################
# Check if the admin servlet was added. 
# Add it if not. 
# It will be needed to stop jetty cleanly.
#####################################################
HAS_ADMIN=`cat $CONFIGS | grep "admin.xml"`
if [ -z "$HAS_ADMIN" ]  
then
  CONFIGS="${CONFIGS} ${JETTY_HOME}/etc/admin.xml"
fi

#####################################################
# Check where logs should go.
#####################################################
if [ -z "$JETTY_LOG" ] 
then
  JETTY_LOG="${JETTY_HOME}/logs"
fi

#####################################################
# Find a location for the pid file
#####################################################
if [  -z "$JETTY_RUN" ] 
then
  JETTY_RUN=`findDirectory -w /var/run /usr/var/run /tmp`
fi

#####################################################
# Find a location for the jetty console
#####################################################
if [  -z "$JETTY_CONSOLE" ] 
then
  JETTY_CONSOLE=/dev/console
fi


##################################################
# Check for JAVA_HOME
##################################################
if [ -z "$JAVA_HOME" ]
then
    # If a java runtime is not defined, search the following
    # directories for a JVM and sort by version. Use the highest
    # version number.

    # Java search path
    JAVA_LOCATIONS="\
        /usr/bin \
        /usr/local/bin \
        /usr/local/java \
        /usr/local/jdk \
        /usr/local/jre \
        /opt/java \
        /opt/jdk \
        /opt/jre \
    " 
    JAVA_NAMES="java jre kaffe"
    for N in $JAVA_NAMES ; do
        for L in $JAVA_LOCATIONS ; do
            [ -d $L ] || continue 
            find $L -name "$N" ! -type d | grep -v threads | while read J ; do
                [ -x $J ] || continue
                VERSION=`eval $J -version 2>&1`       
                [ $? = 0 ] || continue
                VERSION=`expr "$VERSION" : '.*"\(1.[0-9\.]*\)"'`
                [ "$VERSION" = "" ] && continue
                expr $VERSION \< 1.2 >/dev/null && continue
                echo $VERSION:$J
            done
        done
    done | sort | tail -1 > $TMPJ
    JAVA=`cat $TMPJ | cut -d: -f2`
    JVERSION=`cat $TMPJ | cut -d: -f1`

    JAVA_HOME=`dirname $JAVA`
    while [ ! -z "$JAVA_HOME" -a "$JAVA_HOME" != "/" -a ! -f "$JAVA_HOME/lib/tools.jar" ] ; do
        JAVA_HOME=`dirname $JAVA_HOME`
    done
    [ "$JAVA_HOME" = "" ] && JAVA_HOME=

    echo "Found JAVA=$JAVA in JAVA_HOME=$JAVA_HOME"
fi


##################################################
# Determine which JVM of version >1.2
# Try to use JAVA_HOME
##################################################
if [ "$JAVA" = "" -a "$JAVA_HOME" != "" ]
then
  if [ ! -z "$JAVACMD" ] 
  then
     JAVA="$JAVACMD" 
  else
    [ -x $JAVA_HOME/bin/jre -a ! -d $JAVA_HOME/bin/jre ] && JAVA=$JAVA_HOME/bin/jre
    [ -x $JAVA_HOME/bin/java -a ! -d $JAVA_HOME/bin/java ] && JAVA=$JAVA_HOME/bin/java
  fi
fi

if [ "$JAVA" = "" ]
then
    echo "Cannot find a JRE or JDK. Please set $JAVA_HOME to a >1.2 JRE" 2>&2
    exit 1
fi

#####################################################
# Are we running on Windows? Could be, with Cygwin/NT.
#####################################################
if [ -z "$WINBOOTDIR" ] 
then
  PATH_SEPARATOR=":" 
else
  PATH_SEPARATOR=";" 
fi


#####################################################
# Build the classpath with Jetty's bundled libraries.
#####################################################
CP=`ls $JETTY_HOME/lib/*.jar | paste -s -d'$PATH_SEPARATOR' - `
[ "$CLASSPATH" != "" ] && CP=$CP$PATH_SEPARATOR$CLASSPATH
[ -f $JAVA_HOME/lib/tools.jar ] && CP="$CP$PATH_SEPARATOR$JAVA_HOME/lib/tools.jar"
CLASSPATH="$CP"

#####################################################
# Add jetty properties to Java VM options.
#####################################################
JAVA_OPTIONS="-Djetty.home=$JETTY_HOME -Djetty.log=$JETTY_LOG $JAVA_OPTIONS"

#####################################################
# This is how the Jetty server will be started
#####################################################

case "$ACTION" in
  start)
     RUN_CMD="$JAVA -DLOG_FILE=$JETTY_LOG/yyyy_mm_dd.jetty.log -cp $CLASSPATH $JAVA_OPTIONS com.mortbay.Jetty.Server $CONFIGS"
  ;;
  *)
     RUN_CMD="$JAVA -cp $CLASSPATH $JAVA_OPTIONS com.mortbay.Jetty.Server $CONFIGS"
  ;;
esac

#####################################################
# Comment these out after you're happy with what 
# the script is doing.
#####################################################
#echo "JETTY_HOME     =  $JETTY_HOME"
#echo "JETTY_CONF     =  $JETTY_CONF"
#echo "JETTY_LOG      =  $JETTY_LOG"
#echo "JETTY_RUN      =  $JETTY_RUN"
#echo "JETTY_CONSOLE  =  $JETTY_CONSOLE"
#echo "CONFIGS        =  $CONFIGS"
#echo "PATH_SEPARATOR =  $PATH_SEPARATOR"
#echo "JAVA_OPTIONS   =  $JAVA_OPTIONS"
#echo "JAVA           =  $JAVA"
#echo "CLASSPATH      =  $CLASSPATH"


##################################################
# Do the action
##################################################
case "$ACTION" in
  start)
        echo "Starting Jetty: "

        if [ -f $JETTY_RUN/jetty.pid ]
        then
            echo "Already Running!!"
            exit 1
        fi

        echo "STARTED Jetty `date`" >> $JETTY_CONSOLE

        nohup sh -c "exec $RUN_CMD >>$JETTY_CONSOLE 2>&1" >/dev/null &
        echo $! > $JETTY_RUN/jetty.pid
        echo "Jetty running pid="`cat $JETTY_RUN/jetty.pid`
        ;;

  stop)
        PID=`cat $JETTY_RUN/jetty.pid 2>/dev/null`
        echo "Shutting down Jetty: $PID"
        kill $PID 2>/dev/null
        sleep 2
        kill -9 $PID 2>/dev/null
        rm -f $JETTY_RUN/jetty.pid
        echo "STOPPED `date`" >>$JETTY_CONSOLE
        ;;

  restart)
        $0 stop $*
        sleep 5
        $0 start $*
        ;;

  run)
        echo "Running Jetty: "

        if [ -f $JETTY_RUN/jetty.pid ]
        then
            echo "Already Running!!"
            exit 1
        fi

        $RUN_CMD
        ;;

  check)
        echo "Checking arguments to Jetty: "
        echo "JETTY_HOME     =  $JETTY_HOME"
        echo "JETTY_CONF     =  $JETTY_CONF"
        echo "JETTY_LOG      =  $JETTY_LOG"
        echo "JETTY_RUN      =  $JETTY_RUN"
        echo "JETTY_CONSOLE  =  $JETTY_CONSOLE"
        echo "CONFIGS        =  $CONFIGS"
        echo "PATH_SEPARATOR =  $PATH_SEPARATOR"
        echo "JAVA_OPTIONS   =  $JAVA_OPTIONS"
        echo "JAVA           =  $JAVA"
        echo "CLASSPATH      =  $CLASSPATH"
        echo "RUN_CMD        =  $RUN_CMD"
        echo
        
        if [ -f $JETTY_RUN/jetty.pid ]
        then
            echo "Jetty running pid="`cat $JETTY_RUN/jetty.pid`
            exit 0
        fi
        exit 1
        ;;

*)
        echo "Usage: $0 {start|stop|run|restart|check} [ CONFIGS ... ] "
        exit 1
esac

exit 0


