#!/bin/sh
#
# Startup script for Jetty - Java HTTP Servlet Server
#
# This script is controlled by the following environment variables:
# 
# JAVA       - executable JVM
# JAVA_HOME  - Home of java, specifically $JAVA_HOME/lib/tools.jar
# JETTY_HOME - Home of jetty
# JETTY_ETC  - Jetty configuration file directory
# JETTY_RUN  - Directory for jetty pid file
# JETTY_LOG  - Directory for jetty output log
#
# If any of these variables are not set, then frantic searches are made
# of likely locations to try and set them.
#

# Temp file
TMPJ=/tmp/jetty$$
trap "rm -f $TMPJ" 0

##################################################
# Find directory function
##################################################
findDirectory()
{
    OP=$1
    shift
    for L in $* ; do
        [ $OP $L ] || continue ;
        echo $L
        break;
    done 
}


##################################################
# Determine which JVM of version >1.2
# Try to use JAVA_HOME
##################################################
if [ "$JAVA" = "" -a "$JAVA_HOME" != "" ]
then
    [ -x $JAVA_HOME/bin/jre -a ! -d $JAVA_HOME/bin/jre ] && JAVA=$JAVA_HOME/bin/jre
    [ -x $JAVA_HOME/bin/java -a ! -d $JAVA_HOME/bin/java ] && JAVA=$JAVA_HOME/bin/java
fi

##################################################
# If still no JAVA, do a search and test the version of each JVM found
##################################################
if [ "$JAVA" = "" ]
then
    JAVA_HOME=
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
    JAVA_NAMES="java jre kaffe";
    for N in $JAVA_NAMES ; do
        for L in $JAVA_LOCATIONS ; do
            [ -d $L ] || continue ;
            find $L -name "$N" ! -type d | grep -v threads | while read J ; do
                [ -x $J ] || continue;
                VERSION=`eval $J -version 2>&1`       
                [ $? = 0 ] || continue;
                VERSION=`expr "$VERSION" : '.*"\(1.[0-9\.]*\)"'`
                [ "$VERSION" = "" ] && continue;
                expr $VERSION \< 1.2 >/dev/null && continue
                echo $VERSION:$J
            done
        done
    done | sort | tail -1 > $TMPJ
    JAVA=`cat $TMPJ | cut -d: -f2`
    JVERSION=`cat $TMPJ | cut -d: -f1`

    JAVA_HOME=`dirname $JAVA`
    while [ "$JAVA_HOME" != / -a ! -f $JAVA_HOME/lib/tools.jar ] ; do
        JAVA_HOME=`dirname $JAVA_HOME`
    done
    [ "$JAVA_HOME" = "" ] && JAVA_HOME=
fi


##################################################
# if no JETTY_HOME, search likely locations allowing for versions.
##################################################
if [ "$JETTY_HOME" = "" ]
then
    # If no JETTY_HOME defined search the following locations
    # for a jetty release.  Use the most recently modified.

    # Jetty search path
    JETTY_LOCATIONS="\
        $HOME \
        /usr/share/java \
        /usr/local/share/java \
        /usr/local/jetty \
    " 
    for L in $JETTY_LOCATIONS ; do
        [ -d $L ] || continue ;
        find $L -name "[jJ]etty*" -type d | while read J ; do
            [ -f $J/lib/com.mortbay.jetty.jar ] || continue ;
            echo $J
        done
    done | xargs ls -dt1 | head -1 > $TMPJ
  
    JETTY_HOME=`cat $TMPJ`
fi

##################################################
# Find the easy directories
##################################################
[ "$JETTY_ETC" = "" ] && JETTY_ETC=`findDirectory -d /etc/jetty /usr/local/etc/jetty $JETTY_HOME/etc`
[ "$JETTY_RUN" = "" ] && JETTY_RUN=`findDirectory -w /var/run /usr/var/run /tmp`
[ "$JETTY_LOG" = "" ] && JETTY_LOG=`findDirectory -w /var/log /usr/var/log $JETTY_HOME/logs /tmp`

##################################################
# Set the classpath
##################################################
echo `ls $JETTY_HOME/lib/*.jar` | tr ' ' : > $TMPJ
if [ "$CLASSPATH" = "" ]
then
    CLASSPATH=`cat $TMPJ`
else
    CLASSPATH=`cat $TMPJ`:$CLASSPATH
fi
if [ "$JAVA_HOME" = "" ] ; then
    echo "WARNING: JAVA_HOME not set, so cannot locate tools.jar for JSP compilation" >&2
else
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
fi
export CLASSPATH



##################################################
# Get the action & configs
##################################################
ACTION=$1
shift
CONFIGS="$*"


##################################################
# define function to locate configure xml files
##################################################
XmlConfigures()
{
    for C in $1/*.xml ; do
        LC=`head -3 $C | egrep Configure | wc -l`
        [ $LC = 0 ] && continue;
        echo $C
    done | sort
}

##################################################
# Work out what to start
##################################################
if [ "$CONFIGS" = "" ]
then
    CONFIGS=`XmlConfigures $JETTY_ETC`
else
    CTMP=$CONFIGS
    CONFIGS=
    for C in $CTMP ; do
        if [ -f $JETTY_ETC/$C ] ; then
            CONFIGS="$CONFIGS $JETTY_ETC/$C"
        elif  [ -f $JETTY_ETC/$C.xml ] ; then
            CONFIGS="$CONFIGS $JETTY_ETC/$C.xml"
        fi
    done
fi

echo "JAVA       = $JAVA - version $JVERSION"
echo "JAVA_HOME  = $JAVA_HOME"
echo "JETTY_HOME = $JETTY_HOME"
echo "JETTY_ETC  = $JETTY_ETC"
echo "JETTY_RUN  = $JETTY_RUN"
echo "JETTY_LOG  = $JETTY_LOG"
echo "CONFIGS    = `echo $CONFIGS`"
echo "CLASSPATH  = $CLASSPATH"

##################################################
# Do the action
##################################################
case "$ACTION" in
  start)
        echo -n "Starting Jetty: "

        if [ -f $JETTY_RUN/jetty.pid ]
        then
            echo "Already Running!!"
            exit 1
        fi

        cd $JETTY_HOME
       
        echo "STARTED `date`" >>$JETTY_LOG/jetty.out
        nohup $JAVA $JAVA_OPTIONS com.mortbay.Jetty.Server $CONFIGS >>$JETTY_LOG/jetty.out 2>&1 &
        echo $! > $JETTY_RUN/jetty.pid
        ;;

  stop)
        echo -n "Shutting down Jetty: "
        kill `cat $JETTY_RUN/jetty.pid 2>/dev/null` 2>/dev/null
        sleep 2
        kill -9 `cat $JETTY_RUN/jetty.pid 2>/dev/null` 2>/dev/null
        echo `cat $JETTY_RUN/jetty.pid 2>/dev/null`
        rm -f $JETTY_RUN/jetty.pid
        echo "STOPPED `date`" >>$JETTY_LOG/jetty.out
        ;;

  restart)
        $0 stop $*
        sleep 5
        $0 start $*
        ;;
  *)
        echo "Usage: $0 {start|stop|restart} [ servers ... ] "
        exit 1
esac

exit 0