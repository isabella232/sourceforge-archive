#!/bin/sh

#--------------------------------------------------------------
#
# Sample init.d stop/start script for jetty services on Linux.
#
#--------------------------------------------------------------

#
#JAVA_OPTS
#
#This variable can be used to pass args to the jvm
#


#
# DIRNAME, SCRIPTNAME 
#
DIRNAME=`dirname $0`
SCRIPTNAME=`basename $0`

#
# JAVA_HOME
#
if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=/usr/lib/java
  export JAVA_HOME
  echo "JAVA_HOME not set, using default ${JAVA_HOME}" 
fi


#
#JAVA_CMD
#
JAVA_CMD="${JAVA_HOME}/bin/java"


#
# JETTY_HOME
#
if [ -z "$JETTY_HOME" ]; then
  JETTY_HOME=/usr/share/java/jetty
  echo "JETTY_HOME not set, using default ${JETTY_HOME}"
fi


#
# JETTY_XML_FILE
#
if [ -z "$JETTY_XML_FILE" ]; then
  JETTY_XML_FILE=/etc/jetty.xml
  echo "JETTY_XML_FILE not set, using default xml config file: ${JETTY_XML_FILE}"
fi


#
# LOG_DIR
#
if [ -z "$LOG_DIR" ]; then
  LOG_DIR=/var/log
  echo "LOG_DIR not set, using default ${LOG_DIR}"
fi



test -f "${JAVA_CMD}" || exit 0
test -d "${JETTY_HOME}" || exit 0

rotate_logs()
{
  if test -f ${LOG_DIR}/jetty_bootstart_04
  then
    rm ${LOG_DIR}/jetty_bootstart_04
  fi
  if test -f ${LOG_DIR}/jetty_bootstart_03
  then
    mv ${LOG_DIR}/jetty_bootstart_03 ${LOG_DIR}/jetty_bootstart_04
  fi
  if test -f ${LOG_DIR}/jetty_bootstart_02
  then
    mv ${LOG_DIR}/jetty_bootstart_02 ${LOG_DIR}/jetty_bootstart_03
  fi
  if test -f ${LOG_DIR}/jetty_bootstart_01
  then
    mv ${LOG_DIR}/jetty_bootstart_01 ${LOG_DIR}/jetty_bootstart_02
  fi
  if test -f ${LOG_DIR}/jetty_bootstart
  then
    mv ${LOG_DIR}/jetty_bootstart ${LOG_DIR}/jetty_bootstart_01
  fi
}

kill_jetty()
{
  # Guarantee jetty exits
  jetty_pid=""
  for p in `ps -fC java | grep "$SCRIPTNAME" | tr -s " " | cut -f2 -d" "`
  do
    jetty_pid="$jetty_pid $p"
  done

  if test -n "$jetty_pid"
  then
    # Grace
    sleep 5
    kill -4 $jetty_pid >/dev/null 2>&1
    sleep 3
    kill -9 $jetty_pid >/dev/null 2>&1
  fi
}

# Set umask for public read of created files
umask 0022

# set the TZ for java logging
# some java/unix combos don't understand Daylight Savings Time
if test -z "${TZ}"
then
  if test -f /etc/timezone
  then
    TZ="`cat /etc/timezone`"
    export TZ
  elif test -L /etc/localtime
  then
    # Symlink (RedHat style)
    TZ="`ls -l /etc/localtime | sed -e 's%..*/usr/share/zoneinfo/%%g'`"
    export TZ
  elif test -f /etc/localtime
  then
    # Maybe it's a hardlink (SuSE Linux style)
    tz_inode=`ls -i /etc/localtime | cut -f1 -d"/"`
    TZ="`find /usr/share/zoneinfo -inum $tz_inode -print | sed -n 1p | sed -e 's%/usr/share/zoneinfo/%%g'`"
    export TZ
  fi
fi

case "$1" in
start)
    echo -n "Starting Jetty Server: "
    rotate_logs
    nohup $JAVA_CMD $JAVA_OPTS -Djetty.home=${JETTY_HOME} -jar ${JETTY_HOME}/start.jar ${JETTY_XML_FILE} > ${LOG_DIR}/jetty_bootstart 2>&1  &
    echo "done"
    ;;
stop)
    echo -n "Stopping Jetty Server: "
    $JAVA_CMD $JAVA_OPTS -Djetty.home=${JETTY_HOME} -jar ${JETTY_HOME}/stop.jar ${JETTY_XML_FILE} > ${LOG_DIR}/jetty_bootstop 2>&1 
    kill_jetty
    echo "done"
    ;;
restart|force-reload)
    echo -n "Stopping Jetty Server: "
    $JAVA_CMD $JAVA_OPTS -Djetty.home=${JETTY_HOME} -jar ${JETTY_HOME}/stop.jar ${JETTY_XML_FILE} > ${LOG_DIR}/jetty_bootstop 2>&1 
    kill_jetty
    echo "done"
    echo -n "Starting Jetty Server: "
    rotate_logs
    nohup $JAVA_CMD $JAVA_OPTS -Djetty.home=${JETTY_HOME} -jar ${JETTY_HOME}/start.jar ${JETTY_XML_FILE} > ${LOG_DIR}/jetty_bootstart 2>&1  &
    echo "done"
    ;;
*)  echo "Usage: /etc/init.d/${SCRIPTNAME} {start|stop|restart|force-reload}" >&2
    exit 1
    ;;
esac

exit 0
