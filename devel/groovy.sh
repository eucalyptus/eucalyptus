#/bin/bash
TOP=$(readlink -f $(dirname $(dirname ${0})))
CLASSPATH=$(echo $(ls ${TOP}/devel/*.jar ${TOP}/clc/target/*.jar ${TOP}/clc/lib/*.jar | xargs -i echo -n {}:).)
if [ ${#} -eq 0 ]; then
  MAIN="org.codehaus.groovy.tools.shell.Main"
else
  MAIN="groovy.ui.GroovyMain"
fi
java -classpath ${CLASSPATH} \
-Dscript.name=${TOP}/devel/groovy.sh \
-Dprogram.name=groovy \
-Deuca.src.dir=${TOP} \
-Dgroovy.starter.conf=${TOP}/devel/groovy-starter.conf \
-Dgroovy.home=${TOP}/clc/lib/ \
-Dtools.jar=${JAVA_HOME}/lib/tools.jar \
org.codehaus.groovy.tools.GroovyStarter \
--main ${MAIN} \
--conf ${TOP}/devel/groovy-starter.conf \
${@}
