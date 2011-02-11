
# 
# Runs any static method within eucalyptus. Does not require Eucalyptus to be
#  running. Initializes everything, sets up database and persistence contexts, etc.
#
# Syntax: runTest.sh -t fullyQualifiedClassName:staticMethodName[:arg0,arg1...]
#
# Arguments are always of type java.lang.String. If you desire some other
#  type then your method must convert.
#
# Example usage:
#  runTest.sh -t com.eucalyptus.reporting.instance.FalseDataGenerator:summarizeFalseDataTwoCriteria:cluster,user
#

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
echo -e "${CLASSPATH//:/\n}"
java -Xbootclasspath/p:${EUCALYPTUS}/usr/share/eucalyptus/openjdk-crypto.jar -classpath ${CLASSPATH} \
	-Deuca.home=${EUCALYPTUS} \
	-Deuca.lib.dir=${EUCALYPTUS} \
	-Deuca.upgrade.new.dir=${EUCALYPTUS} \
	-Deuca.upgrade.destination=com.eucalyptus.upgrade.MysqldbDestination \
	-Deuca.log.level=TRACE  \
	-Deuca.log.appender=console \
	-Djava.security.egd=file:/dev/./urandom \
	com.eucalyptus.upgrade.TestHarness ${@}
