#!/bin/bash
#
# script for starting a Web service endpoint
# usage: $0 [TCP port]
#

LOG_LEVEL=2 # 0 - critical, 1 - errors, 2 - warnings, 3 - info, 4 - debug, 5- user, 6- trace
PORT=9090

if [[ $EUID -ne 0 ]]; then
    echo WARNING: run this as root if you want to control Xen
fi

if [ -n "$1" ]; then
    PORT=$1
    echo using port $PORT for Web service endpoint
else
    echo using default port $PORT for Web service endpoint
fi

if [ -z $EUCALYPTUS ]; then
    echo ERROR: please, set EUCALYPTUS environment variable
    exit 1
fi

if [ -z $AXIS2C_HOME ]; then
    echo ERROR: please, set AXIS2C_HOME environment variable
    exit 1
fi

if [ -z $LIBVIRT_HOME ]; then
    echo ERROR: please, set LIBVIRT_HOME environment variable
    exit 1
fi


LIB="LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$AXIS2C_HOME/lib:$LIBVIRT_HOME/lib"
echo adding AXIS2C and LIBVIRT to library path:
echo "  $LIB"
export $LIB


CMD="$AXIS2C_HOME/bin/axis2_http_server -p $PORT -r $AXIS2C_HOME -f $AXIS2C_HOME/logs/axis2-port$PORT.log -l $LOG_LEVEL"

while (true); do 
    echo executing the Axis2 server
    echo "  $CMD"
    $CMD
    echo "restarting the server in 2 seconds (press Ctrl-C to interrupt)"
    sleep 2
done
