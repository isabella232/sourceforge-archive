#!/bin/bash

if [ $# != 2 ]
then
    echo "Usage - $0 <sourceforge user name> <version>" >&2
    exit 1
fi

CVS_RSH=ssh
export CVS_RSH

CVS_ARGS=-d$1@cvs.jetty.sourceforge.net:/cvsroot/jetty

VERSION=$2
TAG=$( echo $VERSION | sed 's/\./_/g' )

if [ -d Jetty-$VERSION ]
then
    echo "ERROR: Jetty-$VERSION already exists" >&2
    exit 1
fi

printf "Release %s (%s) (y/n)? " $VERSION $TAG
read Y
[ $Y != "y" ] && exit 1

{
    cd $HOME
    unset JETTY_HOME
    [ -d Jetty ] && mv Jetty Jetty.cvs
    cvs $CVS_ARGS rtag -F Jetty_$TAG Jetty
    cvs $CVS_ARGS export -r Jetty_$TAG Jetty
    cd $HOME/Jetty
    rm -fr src/com webappsrc/com
    ant all tidy || exit 1
    cd ..
    mv Jetty Jetty-$VERSION
    tar cfz /usr/local/archive/Jetty-${VERSION}.tgz --exclude Jetty-$VERSION/classes Jetty-$VERSION
    cd /usr/local/share/java/jetty
    tar xfz /usr/local/archive/Jetty-${VERSION}.tgz

} 2>&1 | tee /tmp/release-jetty.log








