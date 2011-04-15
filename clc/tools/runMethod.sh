
# 
# Runs a static method within eucalyptus. Does not require Eucalyptus to be
#  running beforehand. Initializes everything, sets up database and persistence
#  contexts, etc. Returns whatever return value the method returns. Requires
#  the method to be static, to take only String arguments, and to return an int.
#
# Syntax: runMethod.sh className methodName commaDelimitedArgs
#
# Example usage:
#  runMethod.sh com.eucalyptus.reporting.instance.FalseDataGenerator summarizeFalseDataTwoCriteria cluster,user
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
echo -e "${CLASSPATH//:/\n}"
java -Xms1g -Xmx3g -XX:MaxPermSize=768m -Xbootclasspath/p:${EUCALYPTUS}/usr/share/eucalyptus/openjdk-crypto.jar -classpath ${CLASSPATH} \
	-Deuca.home=${EUCALYPTUS} \
	-Deuca.lib.dir=${EUCALYPTUS} \
	-Deuca.upgrade.new.dir=${EUCALYPTUS} \
	-Deuca.upgrade.destination=com.eucalyptus.upgrade.MysqldbDestination \
	-Deuca.log.level=TRACE  \
	-Deuca.log.appender=console \
	-Djava.security.egd=file:/dev/./urandom \
	com.eucalyptus.upgrade.MethodRunner ${@}
