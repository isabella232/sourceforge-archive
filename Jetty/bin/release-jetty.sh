#!/bin/bash

if [ $# != 2 ]
then
    echo "Usage - $0 <sourceforge user name> <version>" >&2
    exit 1
fi

CVS_RSH=ssh1
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
    [ -d Jetty3 ] && mv Jetty3 Jetty3.$$
    cvs $CVS_ARGS rtag -F Jetty_$TAG Jetty3
    cvs $CVS_ARGS export -r Jetty_$TAG Jetty3
    cd $HOME/Jetty3
    make 
    make install 
    make jars
    make rclean
    rm -fr classes
    mkdir logs    
    rm -fr javadoc
    JETTY_HOME=$HOME/Jetty3 bin/generateJavaDoc 

    cd ..
    mv Jetty3 Jetty-$VERSION
    tar cfz /usr/local/archive/Jetty-${VERSION}.tgz Jetty-$VERSION
    cd /usr/local/java/jetty
    tar xfz /usr/local/archive/Jetty-${VERSION}.tgz

} 2>&1 | tee /tmp/release-jetty.log








