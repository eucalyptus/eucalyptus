#/bin/bash
#
# Copyright 2009-2012 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

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
