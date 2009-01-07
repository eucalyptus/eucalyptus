#!/bin/bash
#
# set up environment variables for running commands linked against Axis
# or libvirt

# get full path of this script
SCRIPT_DIR=`dirname "$0"`
if [ "${SCRIPT_DIR:0:1}" != "/" ]; then
    SCRIPT_DIR="$PWD/$SCRIPT_DIR"
fi
# remove the trailing "/.", if there
SCRIPT_DIR=${SCRIPT_DIR/%\/./}

# assume top-level directory is one level up
# (TODO: won't work if path ends with /..)
EUCALYPTUS_DIR=`dirname $SCRIPT_DIR` 
source $EUCALYPTUS_DIR/Makedefs # to get environment variables

LIB="LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$AXIS2C_HOME/lib:$LIBVIRT_HOME/lib"
echo adding AXIS2C and LIBVIRT to library path:
echo "  $LIB"
export $LIB
