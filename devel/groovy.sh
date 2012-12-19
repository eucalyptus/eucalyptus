#/bin/bash
#
# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

TOP=$(readlink -f $(dirname $(dirname ${0})))
CLASSPATH=$(echo $(ls ${TOP}/devel/*.jar ${TOP}/clc/target/*.jar ${TOP}/clc/lib/*.jar | xargs -i echo -n {}:).)
if [ ${#} -eq 0 ]; then
  MAIN="org.codehaus.groovy.tools.shell.Main"
else
  MAIN="groovy.ui.GroovyMain"
fi
java -classpath ${CLASSPATH} \
-Deuca.home=/tmp \
-Deuca.log.dir=/tmp \
-Deuca.log.level=DEBUG \
-Deuca.log.appender=console \
-Deuca.exhaust.level=DEBUG \
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
