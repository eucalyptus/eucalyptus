#! /bin/bash

# 
# Runs any class in the standard JUnit harness. Does not require Eucalyptus to be
#  running. Initializes everything, sets up database and persistence contexts, etc.
#
# Syntax: runJUnitTest.sh fullyQualifiedClassName
#
# Example usage:
#  runJUnitTest.sh com.eucalyptus.cloud.ws.EucaImagerTest
#
# TODO: unify this with runTest.sh, which does the same thing except it
# runs our own test harness and does not set some env variables

set -e
log() {
  FSTR="${1}\n"
  shift
  printf "${FSTR}" ${@} | tee -a ${UP_LOG}
}

if [ -z "${EUCALYPTUS}" ]; then
	echo "EUCALYPTUS must be set to run tests."
fi

# setup the classpath
CLASSPATH=".:"
FILES=$(\ls -1 ${EUCALYPTUS}/usr/share/eucalyptus/*.jar)
for FILE in $FILES; do
  export CLASSPATH=${FILE}:${CLASSPATH}
done
CLASSPATH=${EUCALYPTUS}/etc/eucalyptus/cloud.d/upgrade:${EUCALYPTUS}/etc/eucalyptus/cloud.d/scripts:${CLASSPATH}
#echo $CLASSPATH

# set a bunch of other variables for euca_imager to work
export VDDK_HOME="$EUCALYPTUS/packages/vddk"
export LD_LIBRARY_PATH="$EUCALYPTUS/packages/axis2c-1.6.0/lib:$EUCALYPTUS/packages/axis2c-1.6.0/modules/rampart:$EUCALYPTUS/usr/lib/eucalyptus:$VDDK_HOME/lib:$VDDK_HOME/lib/vmware-vix-disklib/lib32:$VDDK_HOME/lib/vmware-vix-disklib/lib64/" # to ensure euca_imager finds VDDK libs
export PATH="$EUCALYPTUS/usr/lib/eucalyptus:$PATH" # to ensure euca_imager has euca_rootwrap

# since JUnit expects class names, we assume anything starting with '-' is a JVM argument
for arg in "$@" ; do
    if [ "${arg:0:1}" = '-' ] ; then
        JVM_PARAMS="$JVM_PARAMS $arg"
    else
        JUNIT_PARAMS="$JUNIT_PARAMS $arg"
    fi
done

java -Xbootclasspath/p:${EUCALYPTUS}/usr/share/eucalyptus/openjdk-crypto.jar -classpath ${CLASSPATH} \
    -Deuca.home=${EUCALYPTUS} \
    -Deuca.lib.dir=${EUCALYPTUS} \
    -Deuca.upgrade.new.dir=${EUCALYPTUS} \
    -Deuca.upgrade.destination=com.eucalyptus.upgrade.MysqldbDestination \
    -Deuca.log.level=TRACE  \
    -Deuca.log.appender=console \
    -Djava.security.egd=file:/dev/./urandom \
    ${JVM_PARAMS} \
    org.junit.runner.JUnitCore ${JUNIT_PARAMS}
