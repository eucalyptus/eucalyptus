#!/bin/bash

BASE_URL=http://open.eucalyptus.com/dependencies
POSTFIX=`uname`-`uname -m`
TMP=Makedefs.tmp

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
cd $EUCALYPTUS_DIR # to make sure we're in the right place

function prompt_user_yn {
    while [ 1 ] ; do
      echo -n $1
      read answer
      case "$answer" in
          "y" | "Y" | "yes" | "Yes" | "YES" ) return 1 ;;
          "n" | "N" | "no" | "No" | "NO" ) return 0 ;;
          * ) echo "Input not recognized!" ;;
      esac
    done
}

function get_user_input {
    echo -n $1
    read answer
}

function check_variable {
    local VAR=$1
    local LIB=$2
    local URL=$BASE_URL/$LIB-$POSTFIX.tgz

    echo
    if [ -n "${!VAR}" ]; then
        echo "Environment variable $VAR is set to:"
        echo "    ${!VAR}"
        prompt_user_yn "Would you like to use that value in Makedefs? (y/n): "
        if [ $? == 1 ]; then
            echo "    Adding $VAR to Makedefs"
            # "export" is needed so that programs called by make see these vars
            echo "export $VAR=${!VAR}" >>$TMP 
            return
        fi
    fi
    echo -ne "Please specify a value [$EUCALYPTUS_DIR/lib/$LIB]:"
    read VALUE;
    if [ "$VALUE" == "" ]; then
   	VALUE="$EUCALYPTUS_DIR/lib/$LIB";
    fi
    # "export" is needed so that programs called by make see these vars
    echo "export $VAR=$VALUE" >>$TMP
    if [ -e "$VALUE" ]; then
        echo "Directory $VALUE already exists"
        prompt_user_yn "Do you want to overwrite it with a fresh download? (y/n): "
        if [ $? == 0 ]; then
            echo "Skipping download for $LIB"
            return
        fi
    fi
    echo "Downloading the tarball from"
    echo "   $URL"
    wget -O /tmp/download "$URL"
    echo "Unzipping the tarball..."
    tar -C lib -zxf /tmp/download 
}

if [ -e Makedefs ] ; then
    prompt_user_yn "Makedefs exists, do you want to regenerate it and install dependencies? (y/n): "
    if [ $? == 0 ] ; then
        echo "Assuming that dependencies have been met"
        exit 0
    fi
fi

mkdir -p lib # this is where dependencies will go
echo "# EUCALYPTUS Makedefs generated automatically" >$TMP
echo "" >>$TMP

echo "To build the cloud controller and to install and run Eucalyptus,"
echo "environment variable \$EUCALYPTUS must be defined.  For"
echo "development, we can set it to deployed/ subdirectory in Makedefs."
echo "Alternatively, you can set it by hand to point elsewhere."
prompt_user_yn "Do you want to set \$EUCALYPTUS in Makedefs? (y/n): "
if [ $? == 1 ]; then
    echo "   Adding EUCALYPTUS to Makedefs"
    echo "export EUCALYPTUS=$EUCALYPTUS_DIR/deployed" >>$TMP
fi

# these are the variables that EUCALYPTUS Makefiles
# rely on and the tarballs containing the needed 
# libraries and include files
check_variable AXIS2_HOME "axis2-1.4"
check_variable AXIS2C_HOME "axis2c-bin-1.4.0"
check_variable LIBVIRT_HOME "libvirt-0.4.2"
check_variable GWT_HOME "gwt-1.4.62"

if [ -e Makedefs ] ; then
    echo
    prompt_user_yn "Once again, is it OK to overwrite old Makedefs? (y/n): "
    if [ $? == 1 ] ; then
        mv $TMP Makedefs
    fi
else
    mv $TMP Makedefs
fi

exit 0 # it's all good
